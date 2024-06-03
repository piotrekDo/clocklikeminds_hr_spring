package com.example.clocklike_portal.pto;

import com.example.clocklike_portal.app.Library;
import com.example.clocklike_portal.appUser.AppUserBasicDto;
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

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
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
    private final HolidayOnSaturdayRepository holidayOnSaturdayRepository;
    private final HolidayOnSaturdayUserEntityRepository holidayOnSaturdayUserEntityRepository;
    private Map<String, OccasionalLeaveType> occasionalTypes;

    @PostConstruct
    void init() {
        occasionalTypes = occasionalLeaveTypeRepository.findAll().stream().collect(Collectors.toMap(OccasionalLeaveType::getOccasionalType, Function.identity()));
    }

    List<PtoDto> findAllRequestsByAcceptorId(long id) {
        return ptoRequestsRepository.findAllByAcceptor_appUserId(id).stream()
                .map(ptoTransformer::ptoEntityToDto)
                .toList();
    }

    List<PtoDto> getRequestsForUserForYear(Integer year, Long userId) {
        return ptoRequestsRepository.findRequestsForYear(year, userId).stream()
                .map(ptoTransformer::ptoEntityToDto)
                .toList();
    }

    List<PtoDto> getRequestsForYearForAllUsers(Integer year) {
        return ptoRequestsRepository.findRequestsForYear(year).stream()
                .map(ptoTransformer::ptoEntityToDto)
                .toList();
    }

    List<PtoDto> findAllUnresolvedPtoRequestsByAcceptor(Long id) {
        return ptoRequestsRepository.findAllByDecisionDateTimeIsNullAndAcceptor_AppUserId(id).stream()
                .map(ptoTransformer::ptoEntityToDto)
                .toList();
    }

    List<PtoDto> getRequestsForSupervisorCalendar(Long acceptorId, String start, String end) {
        LocalDate startDate = LocalDate.parse(start, dateFormatter);
        LocalDate endDate = LocalDate.parse(end, dateFormatter);

        return ptoRequestsRepository.findRequestsByAcceptorAndTimeFrame(acceptorId, startDate, endDate).stream()
                .map(ptoTransformer::ptoEntityToDto)
                .toList();
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
            case Library.OCCASIONAL_LEAVE_DISCRIMINATOR_VALUE:
                return processOccasionalLeaveRequest(request, applier, acceptor, startDate, toDate);
            case Library.CHILD_CARE_LEAVE_DISCRIMINATOR_VALUE:
                return processChildCareLeave(request, applier, acceptor, startDate, toDate);
            case Library.ON_SATURDAY_PTO_DISCRIMINATOR_VALUE:
                return processOnSaturdayPtoRequest(request, applier, acceptor, startDate);
            default:
                throw new IllegalOperationException("Unknown request type");
        }
    }

    PtoDto processOnSaturdayPtoRequest(NewPtoRequest request, AppUserEntity applier, AppUserEntity acceptor, LocalDate startDate) {
        LocalDate saturdayRequestDate = LocalDate.parse(request.getSaturdayHolidayDate(), dateFormatter);
        HolidayOnSaturdayEntity holidayEntity = holidayOnSaturdayRepository.findByDate(saturdayRequestDate)
                .orElseThrow(() -> new NoSuchElementException("No such holiday found"));
        HolidayOnSaturdayUserEntity holidayOnSaturdayUserEntity = holidayOnSaturdayUserEntityRepository.findByHolidayAndUser_AppUserId(holidayEntity, applier.getAppUserId())
                .orElseThrow(() -> new NoSuchElementException("No such holiday found for user"));

        if (holidayOnSaturdayUserEntity.getPto() != null) {
            throw new IllegalOperationException("Holiday already used with pto request " + holidayOnSaturdayUserEntity.getPto().getPtoRequestId());
        }

        PtoEntity ptoEntity = ptoRequestsRepository.save(new HolidayOnSaturdayPtoEntity(startDate, applier, acceptor, holidayEntity));
        holidayOnSaturdayUserEntity.setPto(ptoEntity);
        holidayOnSaturdayUserEntityRepository.save(holidayOnSaturdayUserEntity);

        return ptoTransformer.ptoEntityToDto(ptoEntity);
    }

    PtoDto processChildCareLeave(NewPtoRequest request, AppUserEntity applier, AppUserEntity acceptor, LocalDate startDate, LocalDate toDate) {
        String notes = "";
        int businessDays = holidayService.calculateBusinessDays(startDate, toDate);
        if (businessDays != 2) {
            notes = "Niepoprawna liczba dni urlopowych dla wybranego wniosku. Oczekiwana: " + 2 + ", przekazana: " + businessDays + ". ";
        }
        ChildCareLeaveEntity childCareLeaveEntity = new ChildCareLeaveEntity(startDate, toDate, applier, acceptor, businessDays);
        List<PtoEntity> requests = ptoRequestsRepository.findUserRequestsForChildCare(applier.getAppUserId());
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
        } else {
            notes += "Pierwszy wniosek tego typu w bieżącym roku dla użytkownika.";
        }
        childCareLeaveEntity.setNotes(notes);

        return ptoTransformer.ptoEntityToDto(ptoRequestsRepository.save(childCareLeaveEntity));
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

        OccasionalLeaveEntity occasionalLeaveEntity = new OccasionalLeaveEntity(startDate, toDate, applier, acceptor, businessDays, occasionalLeaveType);
        if (notes.isEmpty() && !notes.isBlank()) {
            occasionalLeaveEntity.setNotes(notes);
        }
        return ptoTransformer.ptoEntityToDto(ptoRequestsRepository.save(occasionalLeaveEntity));
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
        } else {
            pto.setNotes("Pierwszy wniosek o urlop na żądanie w tym roku");
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

        if (ptoRequest.getDecisionDateTime() != null) {
            throw new IllegalOperationException("Pto request was already resolved");
        }

        boolean isRequestAccepted = dto.getIsAccepted();

        switch (ptoRequest.getLeaveType()) {
            case Library.PTO_DISCRIMINATOR_VALUE, Library.PTO_ON_DEMAND_DISCRIMINATOR_VALUE:
                resolveStandardPtoRequest(isRequestAccepted, ptoRequest, applier, dto.getDeclineReason());
            case Library.ON_SATURDAY_PTO_DISCRIMINATOR_VALUE:
                resolveSaturdayHolidayPtoRequest(isRequestAccepted, ptoRequest, applier);
        }

        ptoRequest.setWasAccepted(isRequestAccepted);
        ptoRequest.setDecisionDateTime(LocalDateTime.now());
        PtoEntity updatedPtoRequest = ptoRequestsRepository.save(ptoRequest);
        return ptoTransformer.ptoEntityToDto(updatedPtoRequest);
    }

    void resolveSaturdayHolidayPtoRequest(boolean isRequestAccepted, PtoEntity ptoRequest, AppUserEntity applier) {
        if (!isRequestAccepted) {
            if (!(ptoRequest instanceof HolidayOnSaturdayPtoEntity ptoEntity)) {
                throw new IllegalOperationException("Unknown pto entity");
            }
            HolidayOnSaturdayUserEntity holidayOnSaturdayUserEntity = holidayOnSaturdayUserEntityRepository.findByHolidayAndUser_AppUserId(ptoEntity.getHoliday(), applier.getAppUserId())
                    .orElseThrow(() -> new NoSuchElementException("No such holiday found for user"));
            holidayOnSaturdayUserEntity.setPto(null);
            holidayOnSaturdayUserEntityRepository.save(holidayOnSaturdayUserEntity);
        }
    }

    void resolveStandardPtoRequest(boolean isRequestAccepted, PtoEntity ptoEntity, AppUserEntity applier, String declineReason) {
        if (!isRequestAccepted) {
            ptoEntity.setDeclineReason(declineReason);
            int requestBusinessDays = ptoEntity.getBusinessDays();
            int ptoDaysAccruedCurrentYear = applier.getPtoDaysAccruedCurrentYear();
            int ptoDaysLeftCurrentYear = applier.getPtoDaysLeftCurrentYear();
            int fromLastYear = Math.max(0, (ptoDaysLeftCurrentYear + requestBusinessDays) - ptoDaysAccruedCurrentYear);

            applier.setPtoDaysLeftCurrentYear(ptoDaysLeftCurrentYear + (requestBusinessDays - fromLastYear));
            applier.setPtoDaysLeftFromLastYear(applier.getPtoDaysLeftFromLastYear() + fromLastYear);
            applier.setPtoDaysTaken(applier.getPtoDaysTaken() - requestBusinessDays);
        }
    }

    SaturdayHolidayDto registerNewHolidaySaturday(SaturdayHolidayDto request) {
        LocalDate newHoliday = LocalDate.parse(request.getDate(), dateFormatter);
        DayOfWeek dayOfWeek = newHoliday.getDayOfWeek();
        if (dayOfWeek.getValue() != 6) {
            throw new IllegalOperationException("New holiday must be saturday");
        }

        Optional<HolidayOnSaturdayEntity> byDate = holidayOnSaturdayRepository.findByDate(newHoliday);
        if (byDate.isPresent()) {
            throw new IllegalOperationException("Holiday already registered");
        }

        HolidayOnSaturdayEntity holidayEntity = holidayOnSaturdayRepository.save(new HolidayOnSaturdayEntity(newHoliday, request.getNote()));
        List<AppUserEntity> allEmployees = appUserRepository.findAll();

        List<HolidayOnSaturdayUserEntity> holidayUserEntities = new ArrayList<>();
        for (AppUserEntity employee : allEmployees) {
            holidayUserEntities.add(new HolidayOnSaturdayUserEntity(holidayEntity, employee));
        }
        holidayOnSaturdayUserEntityRepository.saveAll(holidayUserEntities);

        return request;
    }

    public HolidayOnSaturdaySummaryDto getHolidaysOnSaturdaySummaryForAdmin() {
        LocalDate now = LocalDate.now();
        HolidayOnSaturdayEntity lastRegistered = holidayOnSaturdayRepository.findFirstByOrderByDateDesc();
        System.out.println("LSAT REG");
        System.out.println(lastRegistered);
        SaturdayHolidayDto nextHolidayOnSaturday = holidayService.findNextHolidayOnSaturday(lastRegistered != null ? lastRegistered.getDate() : null);
        LocalDate nextHolidayDate = LocalDate.parse(nextHolidayOnSaturday.getDate());
        long daysBetween = ChronoUnit.DAYS.between(now, nextHolidayDate);
        List<HolidayOnSaturdayUserEntity> allByHolidayYear = holidayOnSaturdayUserEntityRepository.findAllByHolidayYear(now.getYear());
        List<HolidayOnSaturdayByUserDto> holidaysOnSaturdayByUsers = allByHolidayYear.stream().map(entity -> {
            SaturdayHolidayDto holidayDto = new SaturdayHolidayDto(entity.getHoliday().getId(), entity.getHoliday().getDate().toString(), entity.getHoliday().getNote());
            AppUserBasicDto appUserBasicDto = AppUserBasicDto.appUserEntityToBasicDto(entity.getUser());
            PtoDto ptoDto = entity.getPto() != null ? ptoTransformer.ptoEntityToDto(entity.getPto()) : null;
            return new HolidayOnSaturdayByUserDto(holidayDto, appUserBasicDto, ptoDto);
        }).toList();
        return new HolidayOnSaturdaySummaryDto(nextHolidayOnSaturday, (int) daysBetween, holidaysOnSaturdayByUsers);
    }
}
