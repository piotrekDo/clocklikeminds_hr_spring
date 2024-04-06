package com.example.clocklike_portal.appUser;

import com.example.clocklike_portal.job_position.PositionEntity;
import com.example.clocklike_portal.job_position.PositionHistory;
import com.example.clocklike_portal.job_position.PositionHistoryRepository;
import com.example.clocklike_portal.job_position.PositionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.LocalDate;
import java.util.Optional;

import static com.example.clocklike_portal.appUser.AppUserEntity.createTestAppUser;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
class AppUserServiceTest {

    @Autowired
    AppUserService appUserService;

    @MockBean
    AppUserRepository appUserRepository;

    @MockBean
    UserRoleRepository userRoleRepository;

    @MockBean
    PositionRepository positionRepository;

    @MockBean
    PositionHistoryRepository positionHistoryRepository;

    @TestConfiguration
    static class AppUserServiceTestConfiguration {
        @Bean
        AppUserService appUserService(AppUserRepository appUserRepository, UserRoleRepository userRoleRepository, PositionRepository positionRepository, PositionHistoryRepository positionHistoryRepository) {
            return new AppUserService(appUserRepository, userRoleRepository, positionRepository, positionHistoryRepository);
        }
    }

    @Test
    void updatingUserPositionShouldAddNewUserPositionAdnUpdateHistory() {
        UpdateHireDataRequest request = new UpdateHireDataRequest(1L, "java_dev", null, null, null);
        PositionEntity juniorJavaDevPos = new PositionEntity("junior_java_dev", "Junior Java Developer");
        AppUserEntity appUserEntity = createTestAppUser("firstName", "lastName", "test@mail.com");
        appUserEntity.setActive(true);
        appUserEntity.setPosition(juniorJavaDevPos);
        Mockito.when(appUserRepository.findById(request.getAppUserId())).thenReturn(Optional.of(appUserEntity));
        PositionEntity positionEntity = new PositionEntity("java_dev", "Java Developer");
        Mockito.when(positionRepository.findByPositionKeyIgnoreCase(request.getPositionKey())).thenReturn(Optional.of(positionEntity));
        PositionHistory newPositionHistory = PositionHistory.createNewPositionHistory(positionEntity, LocalDate.now());
        Mockito.when(positionHistoryRepository.save(Mockito.any())).thenReturn(newPositionHistory);
        Mockito.when(appUserRepository.save(appUserEntity)).thenReturn(appUserEntity);

        appUserService.updateHireData(request);

        assertEquals(appUserEntity.getPosition().getDisplayName(), positionEntity.getDisplayName());
        assertEquals(1, appUserEntity.getPositionHistory().size());
    }
}