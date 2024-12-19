package com.example.clocklike_portal.timeoff;

import com.example.clocklike_portal.app.Library;
import com.example.clocklike_portal.appUser.AppUserEntity;
import com.example.clocklike_portal.appUser.AppUserRepository;
import com.example.clocklike_portal.dates_calculations.DateChecker;
import com.example.clocklike_portal.dates_calculations.HolidayService;
import com.example.clocklike_portal.error.IllegalOperationException;
import com.example.clocklike_portal.mail.EmailService;
import com.example.clocklike_portal.timeoff.occasional.OccasionalLeaveEntity;
import com.example.clocklike_portal.timeoff.occasional.OccasionalLeaveType;
import com.example.clocklike_portal.timeoff.occasional.OccasionalLeaveTypeRepository;
import com.example.clocklike_portal.timeoff.on_saturday.*;
import com.example.clocklike_portal.timeoff_history.RequestHistory;
import com.example.clocklike_portal.timeoff_history.RequestHistoryRepository;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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
import static com.example.clocklike_portal.timeoff.PtoEntity.Action.*;

@Component
@RequiredArgsConstructor
public class TimeOffService {

    @PersistenceContext
    private EntityManager entityManager;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private final PtoRepository ptoRequestsRepository;
    private final RequestHistoryRepository requestHistoryRepository;
    private final AppUserRepository appUserRepository;
    private final PtoTransformer ptoTransformer;
    private final HolidayService holidayService;
    private final DateChecker dateChecker;
    private final OccasionalLeaveTypeRepository occasionalLeaveTypeRepository;
    private final HolidayOnSaturdayRepository holidayOnSaturdayRepository;
    private final HolidayOnSaturdayUserEntityRepository holidayOnSaturdayUserEntityRepository;
    private final EmailService emailService;
    private Map<String, OccasionalLeaveType> occasionalTypes;

    @PostConstruct
    void init() {
        occasionalTypes = occasionalLeaveTypeRepository.findAll().stream()
                .collect(Collectors.toMap(OccasionalLeaveType::getOccasionalType, Function.identity()));
    }

    List<TimeOffDto> findAllRequestsByAcceptorId(long id) {
        return ptoRequestsRepository.findAllByAcceptor_appUserId(id).stream()
                .map(ptoTransformer::ptoEntityToDto)
                .toList();
    }

    List<TimeOffDto> getRequestsForUserForYear(Integer year, Long userId) {
        return ptoRequestsRepository.findRequestsForYear(year, userId).stream()
                .map(ptoTransformer::ptoEntityToDto)
                .toList();
    }

    List<TimeOffDto> findAllUnresolvedPtoRequestsByAcceptor(Long id) {
        return ptoRequestsRepository.findUnresolvedOrWithdrawnRequestsByAcceptorId(id).stream()
                .map(ptoTransformer::ptoEntityToDto)
                .toList();
    }

    List<TimeOffDto> getRequestsForSupervisorCalendar(Long acceptorId, String start, String end) {
        LocalDate startDate = LocalDate.parse(start, dateFormatter);
        LocalDate endDate = LocalDate.parse(end, dateFormatter);

        return ptoRequestsRepository.findRequestsByAcceptorAndTimeFrame(acceptorId, startDate, endDate).stream()
                .map(ptoTransformer::ptoEntityToDto)
                .toList();
    }

    Page<TimeOffDto> getPtoRequestsByApplier(Long userId, Integer page, Integer size) {
        page = page == null || page < 0 ? 0 : page;
        size = size == null || size < 1 ? 1000 : size;
        Page<PtoEntity> result = ptoRequestsRepository.findAllByApplier_AppUserId(userId, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "requestDateTime")));
        return result == null ? Page.empty() : result.map(ptoTransformer::ptoEntityToDto);
    }

    Page<TimeOffDto> getPtoRequestsByAcceptor(Long supervisorId, Integer page, Integer size) {
        page = page == null || page < 0 ? 0 : page;
        size = size == null || size < 1 ? 20 : size;
        Page<PtoEntity> result = ptoRequestsRepository.findAllByAcceptor_AppUserId(supervisorId, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "requestDateTime")));
        return result == null ? Page.empty() : result.map(ptoTransformer::ptoEntityToDto);
    }

    PtoSummary getUserPtoSummary(long userId) {
        AppUserEntity user = appUserRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("No user found for applier with ID: " + userId));
        LocalDate now = LocalDate.now();
        List<HolidayOnSaturdayUserEntity> unusedHolidays = holidayOnSaturdayUserEntityRepository.findAllByUserIdAndYearUpToCurrentMonth(userId, now.getYear(), now.getMonthValue());
        return ptoTransformer.createPtoSummary(user, unusedHolidays);
    }

    @Transactional
    TimeOffDto processNewRequest(NewPtoRequest request) {
        TimeOffDto timeOffDto;
        AppUserEntity applier = appUserRepository.findById(request.getApplierId())
                .orElseThrow(() -> new NoSuchElementException("No user found for applier with ID: " + request.getApplierId()));

        if (!applier.isActive()) {
            throw new IllegalOperationException("Applier account is not active.");
        }

        if (applier.isFreelancer() && !Library.PTO_DISCRIMINATOR_VALUE.equals(request.getPtoType())) {
            throw new IllegalOperationException("Cannot request different time off requests than PTO as a freelancer");
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

        if (request.getPtoStart() == null || request.getPtoEnd() == null) {
            throw new IllegalOperationException("Missing pto dates");
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

        int businessDays = holidayService.calculateBusinessDays(startDate, toDate);
        if (businessDays < 1) {
            throw new IllegalOperationException("Pto cannot be less than 1 business day");
        }

        timeOffDto = switch (requestPtoType) {
            case Library.PTO_DISCRIMINATOR_VALUE, Library.PTO_ON_DEMAND_DISCRIMINATOR_VALUE ->
                    processPtoRequest(request, applier, acceptor, startDate, toDate, businessDays);
            case Library.OCCASIONAL_LEAVE_DISCRIMINATOR_VALUE ->
                    processOccasionalLeaveRequest(request, applier, acceptor, startDate, toDate, businessDays);
            case Library.CHILD_CARE_LEAVE_DISCRIMINATOR_VALUE ->
                    processChildCareLeaveRequest(request, applier, acceptor, startDate, toDate, businessDays);
            case Library.ON_SATURDAY_PTO_DISCRIMINATOR_VALUE ->
                    processOnSaturdayPtoRequest(request, applier, acceptor, startDate, businessDays);
            default -> throw new IllegalOperationException("Unknown request type");
        };

        emailService.sendNewTimeOffRequestMailToAcceptor(timeOffDto);
        return timeOffDto;
    }

    TimeOffDto processOnSaturdayPtoRequest(NewPtoRequest request, AppUserEntity applier, AppUserEntity acceptor, LocalDate startDate, int businessDays) {
        if (request.getSaturdayHolidayDate() == null) {
            throw new IllegalOperationException("No holiday specified");
        }
        LocalDate saturdayRequestDate = LocalDate.parse(request.getSaturdayHolidayDate(), dateFormatter);
        HolidayOnSaturdayEntity holidayEntity = holidayOnSaturdayRepository.findByDate(saturdayRequestDate)
                .orElseThrow(() -> new NoSuchElementException("No such holiday found"));
        HolidayOnSaturdayUserEntity holidayOnSaturdayUserEntity = holidayOnSaturdayUserEntityRepository.findByHolidayAndUser_AppUserId(holidayEntity, applier.getAppUserId())
                .orElseThrow(() -> new NoSuchElementException("No such holiday found for user"));

        if (holidayOnSaturdayUserEntity.getPto() != null) {
            throw new IllegalOperationException("Holiday already used with pto request " + holidayOnSaturdayUserEntity.getPto().getPtoRequestId());
        }

        if (businessDays != 1) {
            throw new IllegalOperationException("Pto for saturday holiday must be 1 business day");
        }


        PtoEntity ptoEntity = ptoRequestsRepository.save(new HolidayOnSaturdayPtoEntity(startDate, applier, acceptor, holidayEntity));
        requestHistoryRepository.save(new RequestHistory(null, REGISTER, request.getApplierNotes(), LocalDateTime.now(), applier, ptoEntity));
        holidayOnSaturdayUserEntity.setPto(ptoEntity);
        holidayOnSaturdayUserEntityRepository.save(holidayOnSaturdayUserEntity);

        return ptoTransformer.ptoEntityToDto(ptoEntity);
    }

    TimeOffDto processChildCareLeaveRequest(NewPtoRequest request, AppUserEntity applier, AppUserEntity acceptor, LocalDate startDate, LocalDate toDate, int businessDays) {
        if (startDate.getYear() != toDate.getYear()) {
            throw new IllegalOperationException("Cannot use child care leave at the turn of the year. Please use 2 separate requests");
        }
        if (businessDays > 2) {
            throw new IllegalOperationException("Cannot apply for " + businessDays + ". Maximum days for selected request: " + 2);
        }
        ChildCareLeaveEntity childCareLeaveEntity = new ChildCareLeaveEntity(startDate, toDate, applier, acceptor, businessDays);
        List<PtoEntity> requests = ptoRequestsRepository.findUserRequestsForChildCareAndYear(applier.getAppUserId(), startDate.getYear());
        long totalDaysApplied = requests.stream()
                .filter(pto -> pto.isWasAccepted() || pto.getDecisionDateTime() == null)
                .mapToLong(PtoEntity::getBusinessDays)
                .sum();

        if (totalDaysApplied >= 2) {
            throw new IllegalOperationException("Cannot apply for " + businessDays + ". Maximum days used for current year");
        }

        if (totalDaysApplied > 0 && businessDays > 1) {
            throw new IllegalOperationException("Cannot apply for " + businessDays + ". 1 day left");
        }
        ChildCareLeaveEntity savedTimeOffEntity = ptoRequestsRepository.save(childCareLeaveEntity);
        requestHistoryRepository.save(new RequestHistory(null, REGISTER, request.getApplierNotes(), LocalDateTime.now(), applier, savedTimeOffEntity));
        return ptoTransformer.ptoEntityToDto(savedTimeOffEntity);
    }

    TimeOffDto processOccasionalLeaveRequest(NewPtoRequest request, AppUserEntity applier, AppUserEntity acceptor, LocalDate startDate, LocalDate toDate, int businessDays) {
        if (request.getOccasionalType() == null) {
            throw new IllegalOperationException("No occasional type specified");
        }

        OccasionalLeaveType occasionalLeaveType = occasionalTypes.get(request.getOccasionalType());
        if (occasionalLeaveType == null) {
            throw new NoSuchElementException("Unknown occasional type leave");
        }

        if (businessDays > occasionalLeaveType.getDays()) {
            throw new IllegalOperationException("Cannot apply for " + businessDays + ". Maximum days for selected request: " + occasionalLeaveType.getDays());
        }


        OccasionalLeaveEntity occasionalLeaveEntity = new OccasionalLeaveEntity(startDate, toDate, applier, acceptor, businessDays, occasionalLeaveType);
        OccasionalLeaveEntity savedTimeOffEntity = ptoRequestsRepository.save(occasionalLeaveEntity);
        requestHistoryRepository.save(new RequestHistory(null, REGISTER, request.getApplierNotes(), LocalDateTime.now(), applier, savedTimeOffEntity));
        return ptoTransformer.ptoEntityToDto(savedTimeOffEntity);
    }

    TimeOffDto processPtoRequest(NewPtoRequest request, AppUserEntity applier, AppUserEntity acceptor, LocalDate startDate, LocalDate toDate, int businessDays) {
        int ptoDaysFromLastYear = applier.getPtoDaysLeftFromLastYear();
        int ptoDaysCurrentYear = applier.getPtoDaysLeftCurrentYear();
        int ptoDaysTaken = applier.getPtoDaysTaken();

        if ((ptoDaysCurrentYear + ptoDaysFromLastYear) < businessDays) {
            throw new IllegalOperationException("Insufficient pto days left");
        }

        int subtractedFromLastYearPool = ptoDaysFromLastYear == 0 ? 0 : Math.min(ptoDaysFromLastYear, businessDays);
        int subtractedFromCurrentYearPool = (businessDays - subtractedFromLastYearPool);

        PtoEntity ptoEntityRaw = ptoTransformer.ptoEntityFromNewRequest(request.getPtoType(), startDate, toDate, applier, acceptor, businessDays, subtractedFromLastYearPool);
        if (request.getPtoType().equals(Library.PTO_ON_DEMAND_DISCRIMINATOR_VALUE)) {
            processOnDemandPtoRequest(ptoEntityRaw);
        }
        PtoEntity savedPtoEntity = ptoRequestsRepository.save(ptoEntityRaw);
        applier.setPtoDaysLeftFromLastYear(ptoDaysFromLastYear - subtractedFromLastYearPool);
        requestHistoryRepository.save(new RequestHistory(null, REGISTER, request.getApplierNotes(), LocalDateTime.now(), applier, savedPtoEntity));
        applier.setPtoDaysLeftCurrentYear(ptoDaysCurrentYear - subtractedFromCurrentYearPool);
        applier.setPtoDaysTaken(ptoDaysTaken + businessDays);
        applier.getPtoRequests().add(savedPtoEntity);
        acceptor.getPtoAcceptor().add(savedPtoEntity);
        appUserRepository.saveAll(List.of(applier, acceptor));

        return ptoTransformer.ptoEntityToDto(savedPtoEntity);
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
            pto.setApplicationNotes("Wniosków na żądanie od początku roku: " + totalRequests + ", w tym zaakceptowanych: " + accepted +
                    ". Łącznie zaakceptowanych dni urlopu na żądanie: " + totalDaysAccepted);
        } else {
            pto.setApplicationNotes("Pierwszy wniosek o urlop na żądanie w tym roku");
        }
    }

    @Transactional
    TimeOffDto resolveRequest(ResolvePtoRequest dto) {
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

        if (ptoRequest.isWasMarkedToWithdraw()) {
            resolveWithdrawRequest(dto, ptoRequest, acceptor);
            return null;
        }

        if (ptoRequest.getDecisionDateTime() != null) {
            throw new IllegalOperationException("Pto request was already resolved");
        }

        LocalDateTime now = LocalDateTime.now();
        boolean isRequestAccepted = dto.getIsAccepted();

        switch (ptoRequest.getLeaveType()) {
            case Library.PTO_DISCRIMINATOR_VALUE, Library.PTO_ON_DEMAND_DISCRIMINATOR_VALUE ->
                    resolveStandardPtoRequest(isRequestAccepted, ptoRequest, applier);
            case Library.ON_SATURDAY_PTO_DISCRIMINATOR_VALUE ->
                    resolveSaturdayHolidayPtoRequest(isRequestAccepted, ptoRequest, applier);
        }

        ptoRequest.setWasAccepted(isRequestAccepted);
        if (!isRequestAccepted) {
            ptoRequest.setDeclineReason(dto.getNotes());
        }
        requestHistoryRepository.save(new RequestHistory(null, isRequestAccepted ? ACCEPTED : DECLINED, dto.getNotes(), now, acceptor, ptoRequest));
        ptoRequest.setDecisionDateTime(now);
        PtoEntity updatedPtoRequest = ptoRequestsRepository.save(ptoRequest);

        if (updatedPtoRequest.isWasAccepted()) {
            emailService.sendTimeOffRequestMailConformation(updatedPtoRequest, !applier.isFreelancer());
        } else {
            emailService.sendTimeOffRequestDeniedMailToApplier(updatedPtoRequest);
        }


        return ptoTransformer.ptoEntityToDto(updatedPtoRequest);
    }


    void resolveWithdrawRequest(ResolvePtoRequest resolveRequest, PtoEntity timeOffEntity, AppUserEntity acceptor) {
        if (resolveRequest.getIsAccepted()) {
            clearWithdrawnTimeOffEntityAndSetAsWithdrawn(timeOffEntity, resolveRequest.getNotes());
        } else {
            timeOffEntity.setWasMarkedToWithdraw(false);
            requestHistoryRepository.save(new RequestHistory(null, WITHDRAW_DECLINED, resolveRequest.getNotes(), LocalDateTime.now(), acceptor, timeOffEntity));
            ptoRequestsRepository.save(timeOffEntity);
        }
    }

    void resolveSaturdayHolidayPtoRequest(boolean isRequestAccepted, PtoEntity ptoRequest, AppUserEntity applier) {
        if (isRequestAccepted) return;
        declineSaturdayHolidayRequestAndRestore(ptoRequest, applier);
    }

    void declineSaturdayHolidayRequestAndRestore(PtoEntity ptoRequest, AppUserEntity applier) {
        if (!(ptoRequest instanceof HolidayOnSaturdayPtoEntity ptoEntity)) {
            throw new IllegalOperationException("Unknown pto entity");
        }
        HolidayOnSaturdayUserEntity holidayOnSaturdayUserEntity = holidayOnSaturdayUserEntityRepository.findByHolidayAndUser_AppUserId(ptoEntity.getHoliday(), applier.getAppUserId())
                .orElseThrow(() -> new NoSuchElementException("No such holiday found for user"));
        holidayOnSaturdayUserEntity.setPto(null);
        holidayOnSaturdayUserEntityRepository.save(holidayOnSaturdayUserEntity);
    }

    void resolveStandardPtoRequest(boolean isRequestAccepted, PtoEntity ptoEntity, AppUserEntity applier) {
        if (isRequestAccepted) return;
        declinePtoAndRestoreAppliersDaysLeft(ptoEntity, applier);
    }

    void declinePtoAndRestoreAppliersDaysLeft(PtoEntity ptoEntity, AppUserEntity applier) {
        int requestBusinessDays = ptoEntity.getBusinessDays();
        int ptoDaysAccruedCurrentYear = applier.getPtoDaysAccruedCurrentYear();
        int ptoDaysLeftCurrentYear = applier.getPtoDaysLeftCurrentYear();
        int fromLastYear = Math.max(0, (ptoDaysLeftCurrentYear + requestBusinessDays) - ptoDaysAccruedCurrentYear);

        applier.setPtoDaysLeftCurrentYear(ptoDaysLeftCurrentYear + (requestBusinessDays - fromLastYear));
        applier.setPtoDaysLeftFromLastYear(applier.getPtoDaysLeftFromLastYear() + fromLastYear);
        applier.setPtoDaysTaken(applier.getPtoDaysTaken() - requestBusinessDays);
    }

    @Transactional
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
        List<AppUserEntity> allEmployees = appUserRepository.findAllByFreelancerIsFalse();

        List<HolidayOnSaturdayUserEntity> holidayUserEntities = new ArrayList<>();
        for (AppUserEntity employee : allEmployees) {
            holidayUserEntities.add(new HolidayOnSaturdayUserEntity(holidayEntity, employee));
        }
        holidayOnSaturdayUserEntityRepository.saveAll(holidayUserEntities);

        return request;
    }

    HolidayOnSaturdaySummaryDto getHolidaysOnSaturdaySummaryForAdmin(Integer year) {
        LocalDate now = LocalDate.now();
        int selectedYear = year != null ? year : now.getYear();
        HolidayOnSaturdayEntity lastRegistered = holidayOnSaturdayRepository.findFirstByOrderByDateDesc();
        SaturdayHolidayDto nextHolidayOnSaturday = holidayService.findNextHolidayOnSaturday(lastRegistered != null ? lastRegistered.getDate() : null);
        LocalDate nextHolidayDate = LocalDate.parse(nextHolidayOnSaturday.getDate());
        long daysBetween = ChronoUnit.DAYS.between(now, nextHolidayDate);
        List<SaturdayHolidayDto> saturdayHolidaysSelectedYear = holidayOnSaturdayRepository.findAllByYearOrderByDateDesc(selectedYear).stream()
                .map(SaturdayHolidayDto::fromEntity)
                .toList();
        return new HolidayOnSaturdaySummaryDto(nextHolidayOnSaturday, (int) daysBetween, saturdayHolidaysSelectedYear);
    }

    @Transactional
    WithdrawResponse withdrawTimeOffRequest(Long timeOffRequestId, String applierNotes) {
        if (timeOffRequestId == null) {
            throw new IllegalOperationException("Missing time off request ID");
        }

        PtoEntity timeOffEntity = ptoRequestsRepository.findById(timeOffRequestId)
                .orElseThrow(() -> new NoSuchElementException("No time off request found with id: " + timeOffRequestId));

        AppUserEntity applier = timeOffEntity.getApplier();
        boolean wasAccepted = timeOffEntity.isWasAccepted();
        LocalDateTime decisionDateTime = timeOffEntity.getDecisionDateTime();
        if (!wasAccepted && decisionDateTime == null) {
            return clearWithdrawnTimeOffEntityAndDeleteEntity(timeOffEntity, timeOffRequestId);
        } else {
            if (timeOffEntity.getPtoEnd().isBefore(LocalDate.now())) {
                throw new IllegalOperationException("Cant withdraw past time off request. Contact your superior.");
            }
            timeOffEntity.setWasMarkedToWithdraw(true);
            requestHistoryRepository.save(new RequestHistory(null, MARKED_WITHDRAW, applierNotes, LocalDateTime.now(), applier, timeOffEntity));
            ptoRequestsRepository.save(timeOffEntity);
            return new WithdrawResponse(timeOffRequestId, applier.getAppUserId(), false, true);
        }
    }

    /**
     * below will restore used pto days or saturday holiday only when wasResolvedAndAccepted is true. This will prevent
     * adding additional days when deleting declined request.
     */
    WithdrawResponse clearWithdrawnTimeOffEntityAndDeleteEntity(PtoEntity timeOffEntity, Long timeOffRequestId) {
        boolean wasResolvedAndAccepted = timeOffEntity.getDecisionDateTime() != null && timeOffEntity.isWasAccepted();
        AppUserEntity applier = timeOffEntity.getApplier();
        AppUserEntity acceptor = timeOffEntity.getAcceptor();
        requestHistoryRepository.deleteAll(timeOffEntity.getHistory());
        if (timeOffEntity instanceof OccasionalLeaveEntity || timeOffEntity instanceof ChildCareLeaveEntity) {
            acceptor.getPtoAcceptor().remove(timeOffEntity);
            ptoRequestsRepository.delete(timeOffEntity);
            return new WithdrawResponse(timeOffRequestId, applier.getAppUserId(), true, false);
        } else if (timeOffEntity instanceof HolidayOnSaturdayPtoEntity) {
            if (!wasResolvedAndAccepted) {
                declineSaturdayHolidayRequestAndRestore(timeOffEntity, applier);
            }
            acceptor.getPtoAcceptor().remove(timeOffEntity);
            ptoRequestsRepository.delete(timeOffEntity);
            return new WithdrawResponse(timeOffRequestId, applier.getAppUserId(), true, false);
        } else {
            if (!wasResolvedAndAccepted) {
                declinePtoAndRestoreAppliersDaysLeft(timeOffEntity, applier);
            }
            acceptor.getPtoAcceptor().remove(timeOffEntity);
            ptoRequestsRepository.delete(timeOffEntity);
            return new WithdrawResponse(timeOffRequestId, applier.getAppUserId(), true, false);
        }
    }

    void clearWithdrawnTimeOffEntityAndSetAsWithdrawn(PtoEntity timeOffEntity, String notes) {
        boolean wasResolvedAndAccepted = timeOffEntity.getDecisionDateTime() != null && timeOffEntity.isWasAccepted();
        AppUserEntity applier = timeOffEntity.getApplier();
        AppUserEntity acceptor = timeOffEntity.getAcceptor();
        requestHistoryRepository.save(new RequestHistory(null, WITHDRAW, notes, LocalDateTime.now(), acceptor, timeOffEntity));
        if (timeOffEntity instanceof OccasionalLeaveEntity || timeOffEntity instanceof ChildCareLeaveEntity) {
            acceptor.getPtoAcceptor().remove(timeOffEntity);
            timeOffEntity.setWasAccepted(false);
            timeOffEntity.setWasWithdrawn(true);
            timeOffEntity.setWithdrawnDateTime(LocalDateTime.now());
            ptoRequestsRepository.save(timeOffEntity);
        } else if (timeOffEntity instanceof HolidayOnSaturdayPtoEntity) {
            if (wasResolvedAndAccepted) {
                declineSaturdayHolidayRequestAndRestore(timeOffEntity, applier);
            }
            acceptor.getPtoAcceptor().remove(timeOffEntity);
            timeOffEntity.setWasAccepted(false);
            timeOffEntity.setWasWithdrawn(true);
            timeOffEntity.setWithdrawnDateTime(LocalDateTime.now());
            ptoRequestsRepository.save(timeOffEntity);
        } else {
            if (wasResolvedAndAccepted) {
                declinePtoAndRestoreAppliersDaysLeft(timeOffEntity, applier);
            }
            acceptor.getPtoAcceptor().remove(timeOffEntity);
            timeOffEntity.setWasAccepted(false);
            timeOffEntity.setWasWithdrawn(true);
            timeOffEntity.setWithdrawnDateTime(LocalDateTime.now());
            ptoRequestsRepository.save(timeOffEntity);
        }
    }

    /**
     * @param supervisorId -1 as admin option to fetch all users, regardless supervisor id
     */
    List<HolidayOnSaturdayByUserDto> getHolidayOnSaturdaySummaryByUsers(Long holidayId, Long supervisorId) {
        if (supervisorId == 0) {
            return holidayOnSaturdayUserEntityRepository.findAllByHoliday_Id(holidayId).stream()
                    .map(entity -> {
                        TimeOffDto timeOffDto = ptoTransformer.ptoEntityToDto(entity.getPto());
                        return HolidayOnSaturdayByUserDto.fromEntity(entity, timeOffDto);
                    })
                    .toList();
        }
        return holidayOnSaturdayUserEntityRepository.findAllByHolidayIdAndSupervisorId(holidayId, supervisorId).stream()
                .map(entity -> {
                    TimeOffDto timeOffDto = ptoTransformer.ptoEntityToDto(entity.getPto());
                    return HolidayOnSaturdayByUserDto.fromEntity(entity, timeOffDto);
                })
                .toList();
    }


    List<TimeOffDto> findTimeOffRequestsForAdmin(Long id, Long employeeId, String employeeEmail,
                                                        Long acceptorId, String acceptorEmail,
                                                        Boolean wasAccepted, Boolean wasRejected, Boolean isPending,
                                                        String requestDateFrom, String requestDateTo,
                                                        String ptoStartFrom, String ptoStartTo,
                                                        String ptoEndFrom, String ptoEndTo, Boolean useOr) {
        return performQuery(id, employeeId, employeeEmail, acceptorId, acceptorEmail,
                wasAccepted, wasRejected, isPending,
                requestDateFrom, requestDateTo, ptoStartFrom, ptoStartTo,
                ptoEndFrom, ptoEndTo, useOr);
    }

    List<TimeOffDto> findTimeOffRequestsForSupervisor(Long id, Long employeeId, String employeeEmail,
                                                             Long acceptorId, String acceptorEmail,
                                                             Boolean wasAccepted, Boolean wasRejected, Boolean isPending,
                                                             String requestDateFrom, String requestDateTo,
                                                             String ptoStartFrom, String ptoStartTo,
                                                             String ptoEndFrom, String ptoEndTo) {

        String currentUserEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        AppUserEntity appUserEntity = appUserRepository.findByUserEmailIgnoreCase(currentUserEmail)
                .orElseThrow(() -> new NoSuchElementException("No user found with email: " + currentUserEmail));

        if (acceptorId == null && acceptorEmail == null) {
            throw new IllegalOperationException("Supervisors must provide either their ID or email!");
        }

        if ((acceptorId != null && !Objects.equals(appUserEntity.getAppUserId(), acceptorId)) ||
                (acceptorEmail != null && !Objects.equals(appUserEntity.getUserEmail(), acceptorEmail))) {
            throw new IllegalOperationException("Supervisors cannot query requests without their own ID or email!");
        }

        return performQuery(id, employeeId, employeeEmail, acceptorId, acceptorEmail,
                wasAccepted, wasRejected, isPending,
                requestDateFrom, requestDateTo, ptoStartFrom, ptoStartTo,
                ptoEndFrom, ptoEndTo, false);
    }

    private List<TimeOffDto> performQuery(Long id, Long employeeId, String employeeEmail,
                                          Long acceptorId, String acceptorEmail,
                                          Boolean wasAccepted, Boolean wasRejected, Boolean isPending,
                                          String requestDateFrom, String requestDateTo,
                                          String ptoStartFrom, String ptoStartTo,
                                          String ptoEndFrom, String ptoEndTo, Boolean useOr) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<PtoEntity> cq = cb.createQuery(PtoEntity.class);
        Root<PtoEntity> ptoRoot = cq.from(PtoEntity.class);

        List<Predicate> predicates = buildPredicates(cb, ptoRoot, id, employeeId, employeeEmail, acceptorId, acceptorEmail,
                wasAccepted, wasRejected, isPending,
                requestDateFrom, requestDateTo, ptoStartFrom, ptoStartTo,
                ptoEndFrom, ptoEndTo);

        cq.where(useOr != null && useOr ? cb.or(predicates.toArray(new Predicate[0]))
                : cb.and(predicates.toArray(new Predicate[0])));

        return entityManager.createQuery(cq).getResultList().stream()
                .map(ptoTransformer::ptoEntityToDto)
                .toList();
    }

    private List<Predicate> buildPredicates(CriteriaBuilder cb, Root<PtoEntity> ptoRoot,
                                            Long id, Long employeeId, String employeeEmail,
                                            Long acceptorId, String acceptorEmail,
                                            Boolean wasAccepted, Boolean wasRejected, Boolean isPending,
                                            String requestDateFrom, String requestDateTo,
                                            String ptoStartFrom, String ptoStartTo,
                                            String ptoEndFrom, String ptoEndTo) {
        List<Predicate> predicates = new ArrayList<>();

        if (id != null) {
            predicates.add(cb.equal(ptoRoot.get("ptoRequestId"), id));
        }
        if (employeeId != null) {
            predicates.add(cb.equal(ptoRoot.get("applier").get("appUserId"), employeeId));
        }
        if (employeeEmail != null && !employeeEmail.isEmpty()) {
            predicates.add(cb.equal(ptoRoot.get("applier").get("userEmail"), employeeEmail));
        }
        if (acceptorId != null) {
            predicates.add(cb.equal(ptoRoot.get("acceptor").get("appUserId"), acceptorId));
        }
        if (acceptorEmail != null && !acceptorEmail.isEmpty()) {
            predicates.add(cb.equal(ptoRoot.get("acceptor").get("userEmail"), acceptorEmail));
        }
        if (wasAccepted != null && wasAccepted) {
            predicates.add(cb.isTrue(ptoRoot.get("wasAccepted")));
        }
        if (wasRejected != null && wasRejected) {
            predicates.add(cb.isFalse(ptoRoot.get("wasAccepted")));
            predicates.add(cb.isNotNull(ptoRoot.get("decisionDateTime")));
        }
        if (isPending != null && isPending) {
            predicates.add(cb.isFalse(ptoRoot.get("wasAccepted")));
            predicates.add(cb.isNull(ptoRoot.get("decisionDateTime")));
        }
        if (requestDateFrom != null) {
            predicates.add(cb.greaterThanOrEqualTo(ptoRoot.get("requestDateTime"), LocalDate.parse(requestDateFrom)));
        }
        if (requestDateTo != null) {
            predicates.add(cb.lessThanOrEqualTo(ptoRoot.get("requestDateTime"), LocalDate.parse(requestDateTo)));
        }
        if (ptoStartFrom != null) {
            predicates.add(cb.greaterThanOrEqualTo(ptoRoot.get("ptoStart"), LocalDate.parse(ptoStartFrom)));
        }
        if (ptoStartTo != null) {
            predicates.add(cb.lessThanOrEqualTo(ptoRoot.get("ptoStart"), LocalDate.parse(ptoStartTo)));
        }
        if (ptoEndFrom != null) {
            predicates.add(cb.greaterThanOrEqualTo(ptoRoot.get("ptoEnd"), LocalDate.parse(ptoEndFrom)));
        }
        if (ptoEndTo != null) {
            predicates.add(cb.lessThanOrEqualTo(ptoRoot.get("ptoEnd"), LocalDate.parse(ptoEndTo)));
        }

        return predicates;
    }
}
