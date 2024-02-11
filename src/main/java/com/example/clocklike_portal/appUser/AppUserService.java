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

    AppUserDto finishRegistration(RegistrationFinishRequest request) {
        AppUserEntity appUserEntity = appUserRepository.findById(request.getAppUserId())
                .orElseThrow(() -> new NoSuchElementException("No user found with id: " + request.getAppUserId()));

        if (appUserEntity.isActive()) {
            throw new IllegalOperationException(String.format("User %s already active", appUserEntity.getUserEmail()));
        }

        PositionEntity positionEntity = jobPositionRepository.findByPositionKeyIgnoreCase(request.getPositionKey())
                .orElseThrow(() -> new NoSuchElementException("No job position found with key: " + request.getPositionKey()));

        appUserEntity.setPosition(positionEntity);
        appUserEntity.setHireStart(LocalDate.parse(request.getHireStart()));
        if(request.getHireEnd() != null) {
            appUserEntity.setHireEnd(LocalDate.parse(request.getHireEnd()));
        }
        appUserEntity.setPtoDaysCurrentYear(request.getPtoDaysTotal());
        appUserEntity.setActive(true);

        PositionHistory savedHistory = positionHistoryRepository.save(createNewPositionHistory(positionEntity, LocalDate.parse(request.getHireStart())));
        appUserEntity.getPositionHistory().add(savedHistory);

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
}
