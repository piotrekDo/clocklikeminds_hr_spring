package com.example.clocklike_portal.appUser;

import com.example.clocklike_portal.error.IllegalOperationException;
import com.example.clocklike_portal.job_position.PositionEntity;
import com.example.clocklike_portal.job_position.PositionHistory;
import com.example.clocklike_portal.job_position.PositionHistoryRepository;
import com.example.clocklike_portal.job_position.PositionRepository;
import com.example.clocklike_portal.security.GooglePrincipal;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static com.example.clocklike_portal.job_position.PositionHistory.createNewPositionHistory;
import static org.springframework.data.domain.Sort.Direction.DESC;

@Service
@AllArgsConstructor
public class AppUserService implements UserDetailsService {

    private final AppUserRepository appUserRepository;
    private final UserRoleRepository userRoleRepository;
    private final PositionRepository jobPositionRepository;
    private final PositionHistoryRepository positionHistoryRepository;

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

    AppUserDto finishRegistration(FinishRegistrationRequest request) {
        AppUserEntity appUserEntity = appUserRepository.findById(request.getAppUserId())
                .orElseThrow(() -> new NoSuchElementException("No user found with id: " + request.getAppUserId()));

        if (appUserEntity.isActive()) {
            throw new IllegalOperationException(String.format("User %s already active", appUserEntity.getUserEmail()));
        }

        if (request.getHireStart() == null) {
            throw new IllegalOperationException("No hire start date specified");
        }

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
        appUserEntity.setActive(true);

        updatePositionHistory(positionEntity, appUserEntity, hireStartLocalDate);

        return AppUserDto.appUserEntityToDto(appUserRepository.save(appUserEntity));
    }

    Page<AppUserBasicDto> findAllUsers(Integer page, Integer size) {
        page = page == null || page < 0 ? 0 : page;
        size = size == null || size < 1 ? 10 : size;

        Page<AppUserEntity> result = appUserRepository.findAll(PageRequest.of(page, size, Sort.by(DESC, "hireStart")));
        return result == null ? Page.empty() : result.map(AppUserBasicDto::appUserEntityToBasicDto);
    }

    AppUserDto getAppUserById(Long id) {
        return AppUserDto.appUserEntityToDto(
                appUserRepository.findById(id)
                        .orElseThrow(() -> new NoSuchElementException("No user found with id " + id))
        );
    }

    AppUserDto updateHireData(UpdateHireDataRequest request) {
        AppUserEntity appUserEntity = appUserRepository.findById(request.getAppUserId())
                .orElseThrow(() -> new NoSuchElementException("No user found with id: " + request.getAppUserId()));

        if (!appUserEntity.isActive()) {
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
            hireEnd = appUserEntity.getHireEnd();
        }

        if (hireEnd != null && hireStart != null) {
            if (hireStart.isAfter(hireEnd)) {
                throw new IllegalOperationException("Hire start cannot be less than hire end date");
            }
        }

        appUserEntity.setHireStart(hireStart);
        appUserEntity.setHireEnd(hireEnd);

        return AppUserDto.appUserEntityToDto(appUserRepository.save(appUserEntity));
    }


    public AppUserDto updateHolidayData(UpdateEmployeeHolidayDataRequest request) {
        AppUserEntity appUserEntity = appUserRepository.findById(request.getAppUserId())
                .orElseThrow(() -> new NoSuchElementException("No user found with id: " + request.getAppUserId()));

        if (!appUserEntity.isActive()) {
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

        if (!appUserEntity.isActive()) {
            throw new IllegalOperationException(String.format("User %s is not active, finish registration first", appUserEntity.getUserEmail()));
        }


        Set<PositionHistory> currentPositionHistory = appUserEntity.getPositionHistory();
        Set<PositionHistory> updatedPositionHistory = new LinkedHashSet<>(currentPositionHistory);

        requests.forEach(req -> {
            Long positionHistoryId = req.getPositionHistoryId();
            Optional<PositionHistory> historyOptional = currentPositionHistory.stream().filter(pos -> Objects.equals(pos.getPositionHistoryId(), positionHistoryId)).findFirst();
            PositionHistory positionHistory = historyOptional.orElseThrow(() -> new IllegalOperationException("No such position found at user's history"));

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

}
