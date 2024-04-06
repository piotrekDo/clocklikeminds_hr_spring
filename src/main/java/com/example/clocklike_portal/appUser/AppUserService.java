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
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;

import static com.example.clocklike_portal.job_position.PositionHistory.createNewPositionHistory;
import static org.springframework.data.domain.Sort.Direction.DESC;

@Service
@AllArgsConstructor
public class AppUserService implements UserDetailsService {

    private final AppUserRepository appUserRepository;
    private final UserRoleRepository userRoleRepository;
    private final PositionRepository jobPositionRepository;
    private final PositionHistoryRepository positionHistoryRepository;

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
        System.out.println(request);
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

    private void updatePositionHistory(PositionEntity positionEntity, AppUserEntity appUserEntity, LocalDate start) {
        PositionHistory savedHistory = positionHistoryRepository.save(createNewPositionHistory(positionEntity, start));
        appUserEntity.getPositionHistory().add(savedHistory);
    }
}
