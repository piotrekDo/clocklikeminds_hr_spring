package com.example.clocklike_portal.pto;

import com.example.clocklike_portal.app.Library;
import com.example.clocklike_portal.appUser.AppUserEntity;
import com.example.clocklike_portal.appUser.AppUserRepository;
import com.example.clocklike_portal.dates_calculations.DateChecker;
import com.example.clocklike_portal.dates_calculations.HolidayService;
import com.example.clocklike_portal.error.IllegalOperationException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.example.clocklike_portal.security.SecurityConfig.ADMIN_AUTHORITY;
import static com.example.clocklike_portal.security.SecurityConfig.SUPERVISOR_AUTHORITY;

@Component
@RequiredArgsConstructor
public class PtoService {
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private final PtoRepository ptoRequestsRepository;
    private final AppUserRepository appUserRepository;
    private final PtoTransformer ptoTransformer;
    private final HolidayService holidayService;
    private final DateChecker dateChecker;
    private final OccasionalLeaveTypeRepository occasionalLeaveTypeRepository;
    private Map<String, OccasionalLeaveType> occasionalTypes;

    @PostConstruct
    void init() {
        occasionalTypes = occasionalLeaveTypeRepository.findAll().stream().collect(Collectors.toMap(OccasionalLeaveType::getOccasionalType, Function.identity()));
    }


    Page<PtoDto> getPtoRequests(Long userId, Integer page, Integer size) {
        page = page == null || page < 0 ? 0 : page;
        size = size == null || size < 1 ? 1000 : size;
        Page<PtoEntity> result = ptoRequestsRepository.findAllByApplier_AppUserId(userId, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "requestDateTime")));
        return result == null ? Page.empty() : result.map(ptoTransformer::ptoEntityToDto);
    }

    PtoSummary getUserPtoSummary(long userId) {
        AppUserEntity user = appUserRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("No user found for applier with ID: " + userId));
        Page<PtoEntity> result = ptoRequestsRepository.findAllByApplier_AppUserId(userId, PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "requestDateTime")));

        return ptoTransformer.createPtoSummary(user, result.getContent());
    }

    PtoDto processNewRequest(NewPtoRequest request) {
        AppUserEntity applier = appUserRepository.findById(request.getApplierId())
                .orElseThrow(() -> new NoSuchElementException("No user found for applier with ID: " + request.getApplierId()));

        if (!applier.isActive()) {
            throw new IllegalOperationException("Applier account is not active.");
        }

        AppUserEntity acceptor = null;
        if (request.getAcceptorId() != null) {
            acceptor = appUserRepository.findById(request.getAcceptorId())
                    .orElseThrow(() -> new NoSuchElementException("No user found for acceptor with ID: " + request.getAcceptorId()));
        } else {
            AppUserEntity appliersSupervisor = applier.getSupervisor();
            if (appliersSupervisor == null) {
                throw new IllegalOperationException("Applier has no supervisor and no other acceptor was provided");
            } else {
                acceptor = appliersSupervisor;
            }
        }

        if (!acceptor.isActive()) {
            throw new IllegalOperationException("Acceptor account is not active.");
        }

        boolean isAcceptorAdmin = acceptor.getUserRoles().stream()
                .filter(r -> r.getRoleName().equals(ADMIN_AUTHORITY))
                .toList()
                .size() == 1;

        boolean isAcceptorSupervisor = acceptor.getUserRoles().stream()
                .filter(r -> r.getRoleName().equals(SUPERVISOR_AUTHORITY))
                .toList()
                .size() == 1;

        if (!(isAcceptorSupervisor || isAcceptorAdmin)) {
            throw new IllegalOperationException("Selected acceptor has no authorities to accept pto requests");
        }

        LocalDate startDate = LocalDate.parse(request.getPtoStart(), dateFormatter);
        LocalDate toDate = LocalDate.parse(request.getPtoEnd(), dateFormatter);
        boolean isPtoRangeValid = dateChecker.checkIfDatesRangeIsValid(startDate, toDate);
        if (!isPtoRangeValid) {
            throw new IllegalOperationException("End date cannot be before start date");
        }

        List<PtoEntity> collidingRequests = ptoRequestsRepository
                .findAllOverlappingRequests(applier, toDate, startDate);
        if (collidingRequests.size() > 0) {
            throw new IllegalOperationException("Request colliding with other pto request");
        }

        String requestPtoType = request.getPtoType();
        if (requestPtoType == null) {
            throw new IllegalOperationException("Request type not provided");
        }
        switch (requestPtoType) {
            case Library.PTO_DISCRIMINATOR_VALUE, Library.PTO_ON_DEMAND_DISCRIMINATOR_VALUE:
                return processPtoRequest(request, applier, acceptor, startDate, toDate);
            case Library.OCCASIONAL_LEAVE_DISCRIMINATOR_VALUE, Library.CHILD_CARE_LEAVE_DISCRIMINATOR_VALUE:
                return processOccasionalLeaveRequest(request, applier, acceptor, startDate, toDate);
            default:
                throw new IllegalOperationException("Unknown request type");
        }
    }

    PtoDto processOccasionalLeaveRequest(NewPtoRequest request, AppUserEntity applier, AppUserEntity acceptor, LocalDate startDate, LocalDate toDate) {
        String notes = "";

        if (request.getOccasionalType() == null) {
            throw new IllegalOperationException("No occasional type specified");
        }

        OccasionalLeaveType occasionalLeaveType = occasionalTypes.get(request.getOccasionalType());
        if (occasionalLeaveType == null) {
            throw new NoSuchElementException("Unknown occasional type leave");
        }

        int businessDays = holidayService.calculateBusinessDays(startDate, toDate);
        if (businessDays != occasionalLeaveType.getDays()) {
            notes = "Niepoprawna liczba dni urlopowych dla wybranego wniosku. Oczekiwana: " + occasionalLeaveType.getDays() + ", przekazana: " + businessDays + ". ";
        }

        if ("child_care".equals(occasionalLeaveType.getOccasionalType())) {
            ChildCareLeaveEntity childCareLeaveEntity = new ChildCareLeaveEntity(startDate, toDate, applier, acceptor, businessDays, occasionalLeaveType);
            List<PtoEntity> requests = ptoRequestsRepository.findUserRequestsForChildCare(acceptor.getAppUserId());
            int totalRequests = requests.size();
            long accepted = requests.stream()
                    .filter(PtoEntity::isWasAccepted)
                    .count();
            long totalDaysAccepted = requests.stream()
                    .filter(PtoEntity::isWasAccepted)
                    .mapToLong(PtoEntity::getBusinessDays)
                    .sum();

            if (totalRequests > 0) {
                notes += "Wniosków o opiekę nad dzieckiem od początku roku: " + totalRequests + ", w tym zaakceptowanych: " + accepted +
                        ". Łącznie zaakceptowanych dni urlopu na żądanie: " + totalDaysAccepted;
            }

            if (notes.isEmpty() && !notes.isBlank()) {
                childCareLeaveEntity.setNotes(notes);
            }
            return ptoTransformer.ptoEntityToDto(ptoRequestsRepository.save(childCareLeaveEntity));

        } else {
            OccasionalLeaveEntity occasionalLeaveEntity = new OccasionalLeaveEntity(startDate, toDate, applier, acceptor, businessDays, occasionalLeaveType);
            if (notes.isEmpty() && !notes.isBlank()) {
                occasionalLeaveEntity.setNotes(notes);
            }
            return ptoTransformer.ptoEntityToDto(ptoRequestsRepository.save(occasionalLeaveEntity));
        }

    }

    PtoDto processPtoRequest(NewPtoRequest request, AppUserEntity applier, AppUserEntity acceptor, LocalDate startDate, LocalDate toDate) {
        int businessDays = holidayService.calculateBusinessDays(startDate, toDate);
        int ptoDaysFromLastYear = applier.getPtoDaysLeftFromLastYear();
        int ptoDaysCurrentYear = applier.getPtoDaysLeftCurrentYear();
        int ptoDaysTaken = applier.getPtoDaysTaken();

        if ((ptoDaysCurrentYear + ptoDaysFromLastYear) < businessDays) {
            throw new IllegalOperationException("Insufficient pto days left");
        }

        int subtractedFromLastYearPool = ptoDaysFromLastYear == 0 ? 0 : Math.min(ptoDaysFromLastYear, businessDays);
        int subtractedFromCurrentYearPool = (businessDays - subtractedFromLastYearPool);

        PtoEntity ptoEntityRaw = ptoTransformer.ptoEntityFromNewRequest(request.getPtoType(), false, null, startDate, toDate, applier, acceptor, businessDays, subtractedFromLastYearPool);
        if (request.getPtoType().equals(Library.PTO_ON_DEMAND_DISCRIMINATOR_VALUE)) {
            processOnDemandPtoRequest(ptoEntityRaw);
        }
        PtoEntity ptoEntity = ptoRequestsRepository.save(ptoEntityRaw);
        applier.setPtoDaysLeftFromLastYear(ptoDaysFromLastYear - subtractedFromLastYearPool);
        applier.setPtoDaysLeftCurrentYear(ptoDaysCurrentYear - subtractedFromCurrentYearPool);
        applier.setPtoDaysTaken(ptoDaysTaken + businessDays);
        applier.getPtoRequests().add(ptoEntity);
        acceptor.getPtoAcceptor().add(ptoEntity);
        appUserRepository.saveAll(List.of(applier, acceptor));

        return ptoTransformer.ptoEntityToDto(ptoEntity);
    }

    void processOnDemandPtoRequest(PtoEntity pto) {
        pto.setDemand(true);
        List<PtoEntity> requests = ptoRequestsRepository.findUserRequestsOnDemandFromCurrentYear(pto.getApplier().getAppUserId());
        int totalRequests = requests.size();
        long accepted = requests.stream()
                .filter(PtoEntity::isWasAccepted)
                .count();
        long totalDaysAccepted = requests.stream()
                .filter(PtoEntity::isWasAccepted)
                .mapToLong(PtoEntity::getBusinessDays)
                .sum();

        if (totalRequests > 0) {
            pto.setNotes("Wniosków na żądanie od początku roku: " + totalRequests + ", w tym zaakceptowanych: " + accepted +
                    ". Łącznie zaakceptowanych dni urlopu na żądanie: " + totalDaysAccepted);
        }
    }

    PtoDto resolveRequest(ResolvePtoRequest dto) {
        PtoEntity ptoRequest = ptoRequestsRepository.findById(dto.getPtoRequestId())
                .orElseThrow(() -> new NoSuchElementException("No such PTO request found"));

        String currentUserEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        AppUserEntity applier = ptoRequest.getApplier();
        AppUserEntity acceptor = ptoRequest.getAcceptor();

        boolean isActiveAcceptor = acceptor.isActive();
        boolean isAdminAcceptor = acceptor.getUserRoles().stream().anyMatch(r -> r.getRoleName().equals(ADMIN_AUTHORITY));
        boolean isSupervisorAcceptor = acceptor.getUserRoles().stream().anyMatch(r -> r.getRoleName().equals(SUPERVISOR_AUTHORITY));
        boolean isMatchingAcceptor = acceptor.getUserEmail().equalsIgnoreCase(currentUserEmail);

        if (!(isActiveAcceptor && (isAdminAcceptor || (isSupervisorAcceptor && isMatchingAcceptor)))) {
            throw new IllegalOperationException("You are not authorized to resolve this PTO request.");
        }

        Boolean isRequestAccepted = dto.getIsAccepted();
        if (!isRequestAccepted) {
            ptoRequest.setDeclineReason(dto.getDeclineReason());
            int requestBusinessDays = ptoRequest.getBusinessDays();
            int ptoDaysAccruedCurrentYear = applier.getPtoDaysAccruedCurrentYear();
            int ptoDaysLeftCurrentYear = applier.getPtoDaysLeftCurrentYear();
            int fromLastYear = Math.max(0, (ptoDaysLeftCurrentYear + requestBusinessDays) - ptoDaysAccruedCurrentYear);

            applier.setPtoDaysLeftCurrentYear(ptoDaysLeftCurrentYear + (requestBusinessDays - fromLastYear));
            applier.setPtoDaysLeftFromLastYear(applier.getPtoDaysLeftFromLastYear() + fromLastYear);
            applier.setPtoDaysTaken(applier.getPtoDaysTaken() - requestBusinessDays);
        }

        ptoRequest.setWasAccepted(isRequestAccepted);
        ptoRequest.setDecisionDateTime(LocalDateTime.now());
        PtoEntity updatedPtoRequest = ptoRequestsRepository.save(ptoRequest);

        return ptoTransformer.ptoEntityToDto(updatedPtoRequest);
    }

    List<PtoDto> findAllRequestsByAcceptorId(long id) {
        return ptoRequestsRepository.findAllByAcceptor_appUserId(id).stream()
                .map(ptoTransformer::ptoEntityToDto)
                .toList();
    }

    public List<PtoDto> getRequestsForUserForYear(Integer year, Long userId) {
        return ptoRequestsRepository.findRequestsForYear(year, userId).stream()
                .map(ptoTransformer::ptoEntityToDto)
                .toList();
    }

    public List<PtoDto> getRequestsForYearForAllUsers(Integer year) {
        return ptoRequestsRepository.findRequestsForYear(year).stream()
                .map(ptoTransformer::ptoEntityToDto)
                .toList();
    }

    List<PtoDto> findAllUnresolvedPtoRequestsByAcceptor(Long id) {
        return ptoRequestsRepository.findAllByDecisionDateTimeIsNullAndAcceptor_AppUserId(id).stream()
                .map(ptoTransformer::ptoEntityToDto)
                .toList();
    }

    public List<PtoDto> getRequestsForSupervisorCalendar(Long acceptorId, String start, String end) {
        LocalDate startDate = LocalDate.parse(start, dateFormatter);
        LocalDate endDate = LocalDate.parse(end, dateFormatter);

        return ptoRequestsRepository.findRequestsByAcceptorAndTimeFrame(acceptorId, startDate, endDate).stream()
                .map(ptoTransformer::ptoEntityToDto)
                .toList();
    }
}
