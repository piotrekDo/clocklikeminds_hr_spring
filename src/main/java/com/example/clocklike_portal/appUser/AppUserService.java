package com.example.clocklike_portal.appUser;

import com.example.clocklike_portal.error.IllegalOperationException;
import com.example.clocklike_portal.job_position.PositionEntity;
import com.example.clocklike_portal.job_position.PositionHistory;
import com.example.clocklike_portal.job_position.PositionHistoryRepository;
import com.example.clocklike_portal.job_position.PositionRepository;
import com.example.clocklike_portal.mail.EmailService;
import com.example.clocklike_portal.timeoff.*;
import com.example.clocklike_portal.security.GooglePrincipal;
import com.example.clocklike_portal.timeoff.on_saturday.HolidayOnSaturdayRepository;
import com.example.clocklike_portal.timeoff.on_saturday.HolidayOnSaturdayUserEntity;
import com.example.clocklike_portal.timeoff.on_saturday.HolidayOnSaturdayUserEntityRepository;
import com.example.clocklike_portal.timeoff_history.RequestHistory;
import com.example.clocklike_portal.timeoff_history.RequestHistoryRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.example.clocklike_portal.job_position.PositionHistory.createNewPositionHistory;
import static com.example.clocklike_portal.security.SecurityConfig.SUPERVISOR_AUTHORITY;
import static org.springframework.data.domain.Sort.Direction.DESC;

@Service
public class AppUserService implements UserDetailsService {

    private final AppUserRepository appUserRepository;
    private final RequestHistoryRepository requestHistoryRepository;
    private final UserRoleRepository userRoleRepository;
    private final PositionRepository jobPositionRepository;
    private final PositionHistoryRepository positionHistoryRepository;
    private final HolidayOnSaturdayRepository holidayOnSaturdayRepository;
    private final HolidayOnSaturdayUserEntityRepository holidayOnSaturdayUserEntityRepository;
    private final PtoTransformer ptoTransformer;
    private final EmailService emailService;
    private UserRole supervisorRole = null;

    public AppUserService(AppUserRepository appUserRepository, RequestHistoryRepository requestHistoryRepository, UserRoleRepository userRoleRepository, PositionRepository jobPositionRepository,
                          PositionHistoryRepository positionHistoryRepository, EmailService emailService, HolidayOnSaturdayRepository holidayOnSaturdayRepository,
                          HolidayOnSaturdayUserEntityRepository holidayOnSaturdayUserEntityRepository, PtoTransformer ptoTransformer) {
        this.appUserRepository = appUserRepository;
        this.requestHistoryRepository = requestHistoryRepository;
        this.userRoleRepository = userRoleRepository;
        this.jobPositionRepository = jobPositionRepository;
        this.positionHistoryRepository = positionHistoryRepository;
        this.holidayOnSaturdayRepository = holidayOnSaturdayRepository;
        this.holidayOnSaturdayUserEntityRepository = holidayOnSaturdayUserEntityRepository;
        this.ptoTransformer = ptoTransformer;
        this.emailService = emailService;
    }

    private void getSupervisorRole() {
        if (supervisorRole == null) {
            this.supervisorRole = userRoleRepository.findByRoleNameIgnoreCase(SUPERVISOR_AUTHORITY)
                    .orElseThrow(() -> new NoSuchElementException("No supervisor role found"));
        }
    }

    private void updatePositionHistory(PositionEntity positionEntity, AppUserEntity appUserEntity, LocalDate start) {
        PositionHistory savedHistory = positionHistoryRepository.save(createNewPositionHistory(positionEntity, start));
        appUserEntity.getPositionHistory().add(savedHistory);
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return new UserDetailsAdapter(appUserRepository.findByUserEmailIgnoreCase(username)
                .orElseThrow(() -> new NoSuchElementException("No user found with username " + username)));
    }

    public AppUserEntity registerNewUser(GooglePrincipal googlePrincipal) {
        AppUserEntity userFromGooglePrincipal = AppUserEntity.createUserFromGooglePrincipal(googlePrincipal);
        UserRole userRole = userRoleRepository.findByRoleNameIgnoreCase("user")
                .orElseThrow(() -> new NoSuchElementException("No user role found"));
        userFromGooglePrincipal.setUserRoles(List.of(userRole));
        return appUserRepository.save(userFromGooglePrincipal);
    }

    @Transactional
    AppUserDto finishRegistration(FinishRegistrationRequest request) {
        AppUserEntity appUserEntity = appUserRepository.findById(request.getAppUserId())
                .orElseThrow(() -> new NoSuchElementException("No user found with id: " + request.getAppUserId()));

        if (appUserEntity.isRegistrationFinished()) {
            throw new IllegalOperationException(String.format("User %s registration is already finished", appUserEntity.getUserEmail()));
        }

        if (request.getHireStart() == null) {
            throw new IllegalOperationException("No hire start date specified");
        }

        if (request.getSupervisorId() == null) {
            throw new IllegalOperationException("No supervisor specified");
        }

        if (request.getPositionKey() == null || request.getPositionKey().isBlank() || request.getPositionKey().isEmpty()) {
            throw new IllegalOperationException("No employee position specified");
        }

        AppUserEntity supervisorEntity = appUserRepository.findById(request.getSupervisorId())
                .orElseThrow(() -> new NoSuchElementException("No such supervisor found"));
        appUserEntity.setSupervisor(supervisorEntity);
        supervisorEntity.getSubordinates().add(appUserEntity);

        PositionEntity positionEntity = jobPositionRepository.findByPositionKeyIgnoreCase(request.getPositionKey())
                .orElseThrow(() -> new NoSuchElementException("No job position found with key: " + request.getPositionKey()));

        appUserEntity.setPosition(positionEntity);
        LocalDate hireStartLocalDate = LocalDate.parse(request.getHireStart());
        appUserEntity.setHireStart(hireStartLocalDate);
        if (request.getHireEnd() != null) {
            LocalDate hireEndLocalDate = LocalDate.parse(request.getHireEnd());
            if (hireStartLocalDate.isAfter(hireEndLocalDate)) {
                throw new IllegalOperationException("Hire start cannot be less than hire end date");
            }
            appUserEntity.setHireEnd(hireEndLocalDate);
        }
        appUserEntity.setPtoDaysAccruedCurrentYear(request.getPtoDaysTotal());
        appUserEntity.setPtoDaysLeftCurrentYear(request.getPtoDaysTotal());
        if (request.isFreelancer()) {
            appUserEntity.setFreelancer(true);
        }
        appUserEntity.setRegistrationFinished(true);
        appUserEntity.setActive(true);

        updatePositionHistory(positionEntity, appUserEntity, hireStartLocalDate);

        if (!appUserEntity.isFreelancer()) {
            List<HolidayOnSaturdayUserEntity> accruedSaturdayHolidays = new ArrayList<>();
            holidayOnSaturdayRepository.findAllByDateGreaterThanEqual(appUserEntity.getHireStart())
                    .forEach(holiday -> accruedSaturdayHolidays.add(new HolidayOnSaturdayUserEntity(holiday, appUserEntity)));
            holidayOnSaturdayUserEntityRepository.saveAll(accruedSaturdayHolidays);
        }

        emailService.sendRegistrationConfirmedMsgForUser(appUserEntity);
        return AppUserDto.appUserEntityToDto(appUserRepository.save(appUserEntity));
    }

    Page<AppUserBasicDto> findAllUsers(Integer page, Integer size) {
        page = page == null || page < 0 ? 0 : page;
        size = size == null || size < 1 ? 10 : size;

        Page<AppUserEntity> result = appUserRepository.findAll(PageRequest.of(page, size, Sort.by(DESC, "hireStart")));
        return result == null ? Page.empty() : result.map(AppUserBasicDto::appUserEntityToBasicDto);
    }

    List<EmployeeInfo> getEmployeesBySupervisorId(Long supervisorId) {
        AppUserEntity supervisor = appUserRepository.findById(supervisorId)
                .orElseThrow(() -> new NoSuchElementException("No user found with id: " + supervisorId));
        boolean isAdmin = supervisor.getUserRoles().stream().map(UserRole::getRoleName).collect(Collectors.toSet()).contains("admin");
        LocalDate today = LocalDate.now();
        LocalDate inWeek = LocalDate.now().plusDays(7);
        if (isAdmin) {
            return appUserRepository.findAllEmployeesForAdmin(today, inWeek);
        } else {
            return appUserRepository.findAllEmployeesBySupervisorId(supervisorId, today, inWeek);
        }
    }

    AppUserDto getAppUserById(Long id) {
        return AppUserDto.appUserEntityToDto(
                appUserRepository.findById(id)
                        .orElseThrow(() -> new NoSuchElementException("No user found with id " + id))
        );
    }

    @Transactional
    AppUserDto updateHireData(UpdateHireDataRequest request) {
        AppUserEntity appUserEntity = appUserRepository.findById(request.getAppUserId())
                .orElseThrow(() -> new NoSuchElementException("No user found with id: " + request.getAppUserId()));

        if (!appUserEntity.isRegistrationFinished()) {
            throw new IllegalOperationException(String.format("User %s is not active, finish registration first", appUserEntity.getUserEmail()));
        }

        LocalDate hireStart = null;
        LocalDate hireEnd = null;
        LocalDate positionChangeDate = request.getPositionChangeDate() != null ? LocalDate.parse(request.getPositionChangeDate()) : LocalDate.now();

        if (request.getPositionKey() != null && (appUserEntity.getPosition() == null || !Objects.equals(appUserEntity.getPosition().getPositionKey(), request.getPositionKey()))) {
            PositionEntity positionEntity = jobPositionRepository.findByPositionKeyIgnoreCase(request.getPositionKey())
                    .orElseThrow(() -> new NoSuchElementException("No job position found with key: " + request.getPositionKey()));
            appUserEntity.setPosition(positionEntity);
            updatePositionHistory(positionEntity, appUserEntity, positionChangeDate);
        }

        if (request.getWorkStartDate() != null) {
            hireStart = LocalDate.parse(request.getWorkStartDate());
        } else {
            hireStart = appUserEntity.getHireStart();
        }

        if (request.getWorkEndDate() != null) {
            hireEnd = LocalDate.parse(request.getWorkEndDate());
        } else {
            hireEnd = null;
        }

        if (hireEnd != null && hireStart != null) {
            if (hireStart.isAfter(hireEnd)) {
                throw new IllegalOperationException("Hire start cannot be less than hire end date");
            }
        }

        if (request.getSupervisorId() != null) {
            if (appUserEntity.getSupervisor() == null || !request.getSupervisorId().equals(appUserEntity.getSupervisor().getAppUserId())) {
                AppUserEntity newSupervisorEntity = appUserRepository.findById(request.getSupervisorId())
                        .orElseThrow(() -> new NoSuchElementException("No such supervisor found"));
                getSupervisorRole();
                if (!newSupervisorEntity.getUserRoles().contains(supervisorRole)) {
                    throw new IllegalOperationException("Selected new supervisor doesn't have supervisor role");
                }
                if (appUserEntity.getSupervisor() != null) {
                    AppUserEntity previousSupervisor = appUserRepository.findById(appUserEntity.getSupervisor().getAppUserId())
                            .orElseThrow(() -> new NoSuchElementException("No such supervisor found"));
                    Set<PtoEntity> previousSupervisorPtoAcceptor = previousSupervisor.getPtoAcceptor();
                    Set<PtoEntity> newSupervisorPtoAcceptor = newSupervisorEntity.getPtoAcceptor();
                    List<PtoEntity> userPtosToTransfer = previousSupervisorPtoAcceptor.stream()
                            .filter(pto -> pto.getApplier().getAppUserId().equals(appUserEntity.getAppUserId()) && pto.getDecisionDateTime() == null)
                            .toList();

                    previousSupervisor.setPtoAcceptor(previousSupervisorPtoAcceptor.stream()
                            .filter(pto -> !pto.getApplier().getAppUserId().equals(appUserEntity.getAppUserId()) || pto.getDecisionDateTime() != null)
                            .collect(Collectors.toSet()));
                    userPtosToTransfer.forEach(ptoEntity -> ptoEntity.setAcceptor(newSupervisorEntity));
                    newSupervisorPtoAcceptor.addAll(userPtosToTransfer);
                    newSupervisorEntity.setPtoAcceptor(newSupervisorPtoAcceptor);
                    previousSupervisor.getSubordinates().remove(appUserEntity);
                }

                appUserEntity.setSupervisor(newSupervisorEntity);
                newSupervisorEntity.getSubordinates().add(appUserEntity);
            }
        }

        if (request.getIsFreelancer() != null) {
            appUserEntity.setFreelancer(request.getIsFreelancer());
        }

        appUserEntity.setHireStart(hireStart);
        appUserEntity.setHireEnd(hireEnd);

        return AppUserDto.appUserEntityToDto(appUserRepository.save(appUserEntity));
    }

    AppUserDto updateHolidayData(UpdateEmployeeHolidayDataRequest request) {
        AppUserEntity appUserEntity = appUserRepository.findById(request.getAppUserId())
                .orElseThrow(() -> new NoSuchElementException("No user found with id: " + request.getAppUserId()));

        if (!appUserEntity.isRegistrationFinished()) {
            throw new IllegalOperationException(String.format("User %s is not active, finish registration first", appUserEntity.getUserEmail()));
        }

        if (request.getPtoTotalDaysNewValue() != null) {
            appUserEntity.setPtoDaysAccruedCurrentYear(request.getPtoTotalDaysNewValue());
        }

        if (request.getPtoDaysAcquiredLastYearNewValue() != null) {
            appUserEntity.setPtoDaysAccruedCurrentYear(appUserEntity.getPtoDaysAccruedCurrentYear() - request.getPtoDaysAcquiredLastYearNewValue());
            appUserEntity.setPtoDaysAccruedLastYear(request.getPtoDaysAcquiredLastYearNewValue());
        }

        int accruedCurrentYear = appUserEntity.getPtoDaysAccruedCurrentYear();
        int accruedLastYear = appUserEntity.getPtoDaysAccruedLastYear();
        int daysTaken = appUserEntity.getPtoDaysTaken();

        int subtractedLastYear = Math.min(accruedLastYear, daysTaken);

        appUserEntity.setPtoDaysLeftFromLastYear(Math.max(0, accruedLastYear - subtractedLastYear));
        appUserEntity.setPtoDaysLeftCurrentYear(Math.max(0, (accruedCurrentYear - (daysTaken - subtractedLastYear))));

        return AppUserDto.appUserEntityToDto(appUserRepository.save(appUserEntity));
    }

    AppUserDto updatePositionHistoryData(List<UpdatePositionHistoryRequest> requests, Long employeeId) {
        AppUserEntity appUserEntity = appUserRepository.findById(employeeId)
                .orElseThrow(() -> new NoSuchElementException("No user found with id: " + employeeId));

        if (!appUserEntity.isRegistrationFinished()) {
            throw new IllegalOperationException(String.format("User %s is not active, finish registration first", appUserEntity.getUserEmail()));
        }


        Set<PositionHistory> currentPositionHistory = appUserEntity.getPositionHistory();
        Set<PositionHistory> updatedPositionHistory = new LinkedHashSet<>(currentPositionHistory);

        requests.forEach(req -> {
            Long positionHistoryId = req.getPositionHistoryId();
            Optional<PositionHistory> historyOptional = currentPositionHistory.stream()
                    .filter(pos -> Objects.equals(pos.getPositionHistoryId(), positionHistoryId))
                    .findFirst();
            PositionHistory positionHistory = historyOptional.
                    orElseThrow(() -> new IllegalOperationException("No such position found at user's history"));

            if (req.getStartDate() == null) {
                if (appUserEntity.getPosition().getPositionKey().equals(positionHistory.getPosition().getPositionKey())) {
                    throw new IllegalOperationException("Cannot remove current position");
                }
                updatedPositionHistory.remove(positionHistory);
            } else {
                LocalDate newDate = LocalDate.parse(req.getStartDate());
                positionHistory.setStartDate(newDate);
                updatedPositionHistory.add(positionHistory);
            }
        });

        appUserEntity.setPositionHistory(updatedPositionHistory);


        return AppUserDto.appUserEntityToDto(appUserRepository.save(appUserEntity));
    }

    @Transactional
    AppUserDto updateUserPermission(UpdateUserPermissionRequest request) {
        AppUserEntity appUserEntity = appUserRepository.findById(request.getAppUserId())
                .orElseThrow(() -> new NoSuchElementException("No user found with id: " + request.getAppUserId()));

        if (!appUserEntity.isRegistrationFinished()) {
            throw new IllegalOperationException(String.format("User %s is not active, finish registration first", appUserEntity.getUserEmail()));
        }

        Collection<UserRole> userRoles = appUserEntity.getUserRoles();
        boolean hasHanged = false;

        if (request.getIsActive() != null) {
            appUserEntity.setActive(request.getIsActive());
            hasHanged = true;
        }

        if (request.getHasSupervisorRole() != null) {
            UserRole supervisorRole = userRoleRepository.findByRoleNameIgnoreCase("supervisor")
                    .orElseThrow(() -> new NoSuchElementException("user role not found"));
            if (request.getHasSupervisorRole() && !userRoles.contains(supervisorRole)) {
                userRoles.add(supervisorRole);
            } else if (!request.getHasSupervisorRole()) {
                Set<PtoEntity> ptoAsAcceptor = appUserEntity.getPtoAcceptor();
                ptoAsAcceptor.forEach(ptoEntity -> {
                    if (ptoEntity.getDecisionDateTime() == null) {

                        int requestBusinessDays = ptoEntity.getBusinessDays();
                        int includingLastYearPool = ptoEntity.getIncludingLastYearPool();
                        AppUserEntity applier = ptoEntity.getApplier();
                        applier.setPtoDaysLeftFromLastYear(applier.getPtoDaysLeftFromLastYear() + includingLastYearPool);
                        applier.setPtoDaysLeftCurrentYear(applier.getPtoDaysLeftCurrentYear() + (requestBusinessDays - includingLastYearPool));
                        applier.setPtoDaysTaken(applier.getPtoDaysTaken() - requestBusinessDays);

                        ptoEntity.setWasAccepted(false);
                        ptoEntity.setDecisionDateTime(LocalDateTime.now());
                        String originalApplicationNotes = ptoEntity.getApplicationNotes();
                        ptoEntity.setApplicationNotes(originalApplicationNotes != null ? originalApplicationNotes + " Wskazany przełożony utracił możliwość rozpatrywania wniosków." : "Wskazany przełożony utracił możliwość rozpatrywania wniosków.");
                        requestHistoryRepository.save(new RequestHistory(null, "DECLINED", "Wskazany przełożony utracił możliwość rozpatrywania wniosków.", LocalDateTime.now(), applier, ptoEntity));

                    }
                });
                userRoles.remove(supervisorRole);
                Set<AppUserEntity> currentSubordinates = appUserEntity.getSubordinates();
                currentSubordinates.forEach(employeeEntity -> employeeEntity.setSupervisor(null));
                appUserEntity.setSubordinates(new LinkedHashSet<>());
            }
            hasHanged = true;
        }

        if (request.getHasAdminPermission() != null) {
            UserRole adminRole = userRoleRepository.findByRoleNameIgnoreCase("admin")
                    .orElseThrow(() -> new NoSuchElementException("user role not found"));

            if (request.getHasAdminPermission() && !userRoles.contains(adminRole)) {
                userRoles.add(adminRole);
            } else if (!request.getHasAdminPermission()) {
                userRoles.remove(adminRole);
            }
            hasHanged = true;
        }

        if (userRoles != null) {
            appUserEntity.setUserRoles(userRoles);
        }

        if (hasHanged) {
            appUserEntity = appUserRepository.save(appUserEntity);
        }

        return AppUserDto.appUserEntityToDto(appUserEntity);
    }

    List<AppUserBasicDto> getAllSupervisors() {
        getSupervisorRole();
        List<AppUserEntity> supervisorEntities = appUserRepository.findAllByUserRolesContaining(supervisorRole);

        return supervisorEntities.stream()
                .map(AppUserBasicDto::appUserEntityToBasicDto)
                .collect(Collectors.toList());
    }

}
