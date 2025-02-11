package com.example.clocklike_portal.timeoff;

import com.example.clocklike_portal.app.Library;
import com.example.clocklike_portal.appUser.AppUserBasicDto;
import com.example.clocklike_portal.appUser.AppUserEntity;
import com.example.clocklike_portal.appUser.AppUserRepository;
import com.example.clocklike_portal.appUser.UserDetailsAdapter;
import com.example.clocklike_portal.dates_calculations.DateChecker;
import com.example.clocklike_portal.dates_calculations.HolidayService;
import com.example.clocklike_portal.error.IllegalOperationException;
import com.example.clocklike_portal.mail.EmailService;
import com.example.clocklike_portal.pdf.PdfCreator;
import com.example.clocklike_portal.timeoff.occasional.OccasionalLeaveEntity;
import com.example.clocklike_portal.timeoff.occasional.OccasionalLeaveType;
import com.example.clocklike_portal.timeoff.occasional.OccasionalLeaveTypeRepository;
import com.example.clocklike_portal.timeoff.on_saturday.*;
import com.example.clocklike_portal.timeoff_history.RequestHistory;
import com.example.clocklike_portal.timeoff_history.RequestHistoryRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.example.clocklike_portal.security.SecurityConfig.ADMIN_AUTHORITY;
import static com.example.clocklike_portal.security.SecurityConfig.SUPERVISOR_AUTHORITY;
import static com.example.clocklike_portal.timeoff.PtoEntity.Action.*;

@Component
@RequiredArgsConstructor
public class TimeOffService {

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
    private final PdfCreator pdfCreator;
    private Map<String, OccasionalLeaveType> occasionalTypes;

    @PostConstruct
    void init() {
        occasionalTypes = occasionalLeaveTypeRepository.findAll().stream()
                .collect(Collectors.toMap(OccasionalLeaveType::getOccasionalType, Function.identity()));
    }

    private static UserDetailsAdapter getUserDetails() {
        SecurityContext context = SecurityContextHolder.getContext();
        return (UserDetailsAdapter) context.getAuthentication().getPrincipal();
    }

    public int calculateBusinessDaysInMonth(LocalDate firstDay, LocalDate lastDay) {
        return holidayService.calculateBusinessDays(firstDay, lastDay);
    }

    public int calculateDaysOnHolidays(List<TimeOffDto> requestsByMonth, int monthIndex, int givenYear) {
        AtomicInteger daysOnHolidays = new AtomicInteger();
        requestsByMonth.stream().filter(r -> r.isWasAccepted() && !r.isWasMarkedToWithdraw() && !r.isWasWithdrawn()).forEach(r -> {
            LocalDate ptoStart = r.getPtoStart();
            LocalDate ptoEnd = r.getPtoEnd();
            LocalDate finalStart = ptoStart.getMonthValue() != monthIndex ? LocalDate.of(givenYear, monthIndex, 1) : ptoStart;
            LocalDate finalEnd = ptoEnd.getMonthValue() != monthIndex ? LocalDate.of(givenYear, monthIndex, 1).with(TemporalAdjusters.lastDayOfMonth()) : ptoEnd;
            daysOnHolidays.addAndGet(holidayService.calculateBusinessDays(finalStart, finalEnd));
        });

        return daysOnHolidays.intValue();
    }

    public List<TimeOffDto> getAcceptedUserRequestsInTimeFrame(long userId, LocalDate start, LocalDate end) {
        return ptoRequestsRepository.findAcceptedRequestsByApplierAndTimeFrame(userId, start, end).stream()
                .map(ptoTransformer::ptoEntityToDto)
                .toList();
    }

    RequestsForUserCalendar getUserCalendarSummary(int year) {
        long userId = getUserDetails().getUserId();
        List<TimeOffDto> allRequests = ptoRequestsRepository.findRequestsForYear(year, userId).stream()
                .map(ptoTransformer::ptoEntityToDto)
                .toList();
        List<List<TimeOffDto>> timeOffLists = new ArrayList<>(12);
        for (int i = 0; i < 12; i++) {
            timeOffLists.add(new ArrayList<>());
        }
        allRequests.forEach(r -> {
            int startMonth = r.getPtoStart().getMonthValue() - 1;
            int endMonth = r.getPtoEnd().getMonthValue() - 1;
            if (startMonth == endMonth) {
                timeOffLists.get(startMonth).add(r);
            } else {
                if (r.getPtoStart().getYear() == year)
                    timeOffLists.get(startMonth).add(r);
                if (r.getPtoEnd().getYear() == year)
                    timeOffLists.get(endMonth).add(r);
            }
        });

        RequestsForUserCalendar result = new RequestsForUserCalendar();
        for (int i = 0; i < timeOffLists.size(); i++) {
            int currentMonth = i + 1;
            List<TimeOffDto> requestsByMonth = timeOffLists.get(i);
            LocalDate firstDay = LocalDate.of(year, currentMonth, 1);
            LocalDate lastDay = firstDay.withDayOfMonth(firstDay.lengthOfMonth());
            int workingDays = calculateBusinessDaysInMonth(firstDay, lastDay);
            int daysOnHolidays = calculateDaysOnHolidays(requestsByMonth, currentMonth, year);
            int workHours = workingDays * 8;
            int daysOnHoliday = daysOnHolidays * 8;
            int workedHours = workHours - daysOnHoliday;
            String monthName = LocalDate.of(year, currentMonth, 1)
                    .getMonth()
                    .getDisplayName(TextStyle.FULL, Locale.ENGLISH);
            result.getMonths().add(new RequestsForUserCalendar.MonthSummary(i + 1, i, monthName, workHours, workedHours, requestsByMonth));
        }
        return result;
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

    List<TimeOffDto> findAllRequestsForYearForUser(Integer year, Long userId) {
        return ptoRequestsRepository.findAllRequestsForYear(year, userId).stream().map(ptoTransformer::ptoEntityToDto)
                .toList();
    }

    List<TimeOffDto> getRequestsForSupervisorCalendar(String start, String end) {
        long requesterId = getUserDetails().getUserId();
        LocalDate startDate = LocalDate.parse(start, dateFormatter);
        LocalDate endDate = LocalDate.parse(end, dateFormatter);

        return ptoRequestsRepository.findRequestsByAcceptorAndTimeFrame(requesterId, startDate, endDate).stream()
                .map(ptoTransformer::ptoEntityToDto)
                .toList();
    }

    Page<TimeOffDto> getPtoRequestsByApplier(Long userId, Integer page, Integer size) {
        page = page == null || page < 0 ? 0 : page;
        size = size == null || size < 1 ? 1000 : size;
        Page<PtoEntity> result = ptoRequestsRepository.findAllByApplier_AppUserIdOrderByPtoRequestIdDesc(userId, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "requestDateTime")));
        return result == null ? Page.empty() : result.map(ptoTransformer::ptoEntityToDto);
    }

    Page<TimeOffDto> getPtoRequestsByAcceptor(Integer page, Integer size) {
        UserDetailsAdapter principal = getUserDetails();
        long userId = principal.getUserId();
        boolean isAdmin = principal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equals(ADMIN_AUTHORITY));
        page = page == null || page < 0 ? 0 : page;
        size = size == null || size < 1 ? 10 : size;
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "requestDateTime"));
        Page<PtoEntity> result = isAdmin ? ptoRequestsRepository.findAll(pageRequest) : ptoRequestsRepository.findAllByAcceptor_AppUserId(userId, pageRequest);
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
        requestHistoryRepository.save(new RequestHistory(null, REGISTER, request.getApplierNotes(), OffsetDateTime.now(ZoneOffset.UTC), applier, ptoEntity));
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
        requestHistoryRepository.save(new RequestHistory(null, REGISTER, request.getApplierNotes(), OffsetDateTime.now(ZoneOffset.UTC), applier, savedTimeOffEntity));
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
        requestHistoryRepository.save(new RequestHistory(null, REGISTER, request.getApplierNotes(), OffsetDateTime.now(ZoneOffset.UTC), applier, savedTimeOffEntity));
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
        requestHistoryRepository.save(new RequestHistory(null, REGISTER, request.getApplierNotes(), OffsetDateTime.now(ZoneOffset.UTC), applier, savedPtoEntity));
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

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
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
            emailService.sendTimeOffRequestMailConformation(updatedPtoRequest, applier.isFreelancer());
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
            requestHistoryRepository.save(new RequestHistory(null, WITHDRAW_DECLINED, resolveRequest.getNotes(), OffsetDateTime.now(ZoneOffset.UTC), acceptor, timeOffEntity));
            ptoRequestsRepository.save(timeOffEntity);
            emailService.sendRequestWithdrawDeclinedMessage(timeOffEntity);
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
        OffsetDateTime decisionDateTime = timeOffEntity.getDecisionDateTime();
        if (!wasAccepted && decisionDateTime == null) {
            return clearWithdrawnTimeOffEntityAndDeleteEntity(timeOffEntity, timeOffRequestId);
        } else {
            if (timeOffEntity.getPtoEnd().isBefore(LocalDate.now())) {
                throw new IllegalOperationException("Cant withdraw past time off request. Contact your superior.");
            }
            timeOffEntity.setWasMarkedToWithdraw(true);
            requestHistoryRepository.save(new RequestHistory(null, MARKED_WITHDRAW, applierNotes, OffsetDateTime.now(ZoneOffset.UTC), applier, timeOffEntity));
            ptoRequestsRepository.save(timeOffEntity);
            emailService.sendRequestMarkedForWithdrawMessage(timeOffEntity);
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
            emailService.sendWithdrawConformationForNotResolvedRequest(timeOffEntity);
            return new WithdrawResponse(timeOffRequestId, applier.getAppUserId(), true, false);
        }
    }

    void clearWithdrawnTimeOffEntityAndSetAsWithdrawn(PtoEntity timeOffEntity, String notes) {
        boolean wasResolvedAndAccepted = timeOffEntity.getDecisionDateTime() != null && timeOffEntity.isWasAccepted();
        AppUserEntity applier = timeOffEntity.getApplier();
        AppUserEntity acceptor = timeOffEntity.getAcceptor();
        requestHistoryRepository.save(new RequestHistory(null, WITHDRAW, notes, OffsetDateTime.now(ZoneOffset.UTC), acceptor, timeOffEntity));
        if (timeOffEntity instanceof OccasionalLeaveEntity || timeOffEntity instanceof ChildCareLeaveEntity) {
            acceptor.getPtoAcceptor().remove(timeOffEntity);
            timeOffEntity.setWasAccepted(false);
            timeOffEntity.setWasWithdrawn(true);
            timeOffEntity.setWithdrawnDateTime(OffsetDateTime.now(ZoneOffset.UTC));
            ptoRequestsRepository.save(timeOffEntity);
        } else if (timeOffEntity instanceof HolidayOnSaturdayPtoEntity) {
            if (wasResolvedAndAccepted) {
                declineSaturdayHolidayRequestAndRestore(timeOffEntity, applier);
            }
            acceptor.getPtoAcceptor().remove(timeOffEntity);
            timeOffEntity.setWasAccepted(false);
            timeOffEntity.setWasWithdrawn(true);
            timeOffEntity.setWithdrawnDateTime(OffsetDateTime.now(ZoneOffset.UTC));
            ptoRequestsRepository.save(timeOffEntity);
        } else {
            if (wasResolvedAndAccepted) {
                declinePtoAndRestoreAppliersDaysLeft(timeOffEntity, applier);
            }
            acceptor.getPtoAcceptor().remove(timeOffEntity);
            timeOffEntity.setWasAccepted(false);
            timeOffEntity.setWasWithdrawn(true);
            timeOffEntity.setWithdrawnDateTime(OffsetDateTime.now(ZoneOffset.UTC));
            ptoRequestsRepository.save(timeOffEntity);
        }
            emailService.sendRequestWithdrawnMessage(timeOffEntity);
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

    List<TimeOffRequestsByEmployee> getRequestsBySupervisorAndTimeframe(String start, String end) {
        long requesterId = getUserDetails().getUserId();
        boolean isAdmin = getUserDetails().getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equals(ADMIN_AUTHORITY));
        LocalDate startDate = LocalDate.parse(start, dateFormatter);
        LocalDate endDate = LocalDate.parse(end, dateFormatter);

        Map<AppUserBasicDto, List<TimeOffDto>> result = new HashMap<>();

        if (!isAdmin) {
            appUserRepository.findAllBySupervisor_AppUserId(requesterId)
                    .forEach(emp -> result.put(
                            AppUserBasicDto.appUserEntityToBasicDto(emp),
                            new ArrayList<>()
                    ));
            result.putAll(ptoRequestsRepository.findRequestsBySupervisorAndTimeFrame(requesterId, startDate, endDate)
                    .stream()
                    .collect(Collectors.groupingBy(
                            ptoEntity -> AppUserBasicDto.appUserEntityToBasicDto(ptoEntity.getApplier()),
                            Collectors.mapping(ptoTransformer::ptoEntityToDto, Collectors.toList())
                    )));
        } else {
            appUserRepository.findAll().forEach(emp -> result.put(
                    AppUserBasicDto.appUserEntityToBasicDto(emp),
                    new ArrayList<>()
            ));
            result.putAll(ptoRequestsRepository.findRequestsInTimeFrame(startDate, endDate)
                    .stream()
                    .collect(Collectors.groupingBy(
                            ptoEntity -> AppUserBasicDto.appUserEntityToBasicDto(ptoEntity.getApplier()),
                            Collectors.mapping(ptoTransformer::ptoEntityToDto, Collectors.toList())
                    )));
        }

        return result.entrySet().stream()
                .map(entry -> new TimeOffRequestsByEmployee(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

    byte[] generateTimeOffPdf(Long timeOffId) {
        long requesterId = getUserDetails().getUserId();
        boolean isAdmin = getUserDetails().getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equals(ADMIN_AUTHORITY));

        PtoEntity ptoEntity = ptoRequestsRepository.findById(timeOffId)
                .orElseThrow(() -> new NoSuchElementException("No time off request found with id " + timeOffId));

        if (!isAdmin && (ptoEntity.getApplier().getAppUserId() != requesterId && ptoEntity.getAcceptor().getAppUserId() != requesterId)) {
            throw new IllegalOperationException("Cannot generate another user request!");
        }

        return pdfCreator.generateTimeOffRequestPdfAsBytes(ptoEntity);
    }

    boolean resendRequestByMail(Long requestId) {
        long requesterId = getUserDetails().getUserId();
        boolean isAdmin = getUserDetails().getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equals(ADMIN_AUTHORITY));
        PtoEntity ptoEntity = ptoRequestsRepository.findById(requestId)
                .orElseThrow(() -> new NoSuchElementException("No time off request found with id " + requestId));

        if (ptoEntity.getAcceptor().isFreelancer()) {
            throw new IllegalOperationException("Cannot generate and resend request for freelancer");
        }

        if (!isAdmin && (ptoEntity.getApplier().getAppUserId() != requesterId && ptoEntity.getAcceptor().getAppUserId() != requesterId)) {
            throw new IllegalOperationException("Cannot resend another user request!");
        }

        emailService.sendTimeOffRequestMailConformation(ptoEntity, false);
        return true;
    }
}
