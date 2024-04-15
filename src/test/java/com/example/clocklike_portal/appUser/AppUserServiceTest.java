package com.example.clocklike_portal.appUser;

import com.example.clocklike_portal.job_position.PositionEntity;
import com.example.clocklike_portal.job_position.PositionHistory;
import com.example.clocklike_portal.job_position.PositionHistoryRepository;
import com.example.clocklike_portal.job_position.PositionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.example.clocklike_portal.appUser.AppUserEntity.createTestAppUser;
import static java.lang.Integer.parseInt;
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
        appUserEntity.setRegistrationFinished(true);
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


    @ParameterizedTest
    @CsvSource(value = {
            "30, 5, 21, 5, 10, 20, 0",
            "21, 0, 0, 0, 20, 1, 0",
            "20, 10, 20, 0, 2, 10, 8"
    })
    void updateHolidayDataShouldSetCorrectValuesForDaysLeft(String totalDaysNewReqVal, String lastYearReqVal, String userCurrYearVal,
                                                            String userLastYearVal, String daysTaken, String userCurrYearDaysLeft, String userLastYearDaysLeft) {
        UpdateEmployeeHolidayDataRequest request = new UpdateEmployeeHolidayDataRequest(1L, parseInt(totalDaysNewReqVal), parseInt(lastYearReqVal));
        AppUserEntity testAppUser = createTestAppUser("first", "last", "mail@mail.com");
        testAppUser.setAppUserId(1L);
        testAppUser.setRegistrationFinished(true);
        testAppUser.setPtoDaysAccruedCurrentYear(parseInt(userCurrYearVal));
        testAppUser.setPtoDaysAccruedLastYear(parseInt(userLastYearVal));
        testAppUser.setPtoDaysTaken(parseInt(daysTaken));

        AppUserDto dto = new AppUserDto(1L, "first", "last", "mail@mail.com", null, true, true, true, null, null, null, null, 0L, parseInt(lastYearReqVal), parseInt(totalDaysNewReqVal), parseInt(userLastYearDaysLeft), parseInt(userCurrYearDaysLeft), parseInt(daysTaken));

        Mockito.when(appUserRepository.findById(1L)).thenReturn(Optional.of(testAppUser));
        Mockito.when(appUserRepository.save(Mockito.any())).thenReturn(testAppUser);

        appUserService.updateHolidayData(request);

        assertEquals(parseInt(userCurrYearDaysLeft), testAppUser.getPtoDaysLeftCurrentYear());
        assertEquals(parseInt(userLastYearDaysLeft), testAppUser.getPtoDaysLeftFromLastYear());
    }

    @Test
    void updatePositionHistoryDataShouldRemoveOneHistoryRecord() {
        AppUserEntity testAppUser = createTestAppUser("first", "last", "mail@mail.com");
        testAppUser.setAppUserId(1L);
        testAppUser.setRegistrationFinished(true);
        PositionEntity testPosition = new PositionEntity(10L, "test_pos", "Test Position");
        PositionHistory positionHistory1 = new PositionHistory(21L, testPosition, LocalDate.of(2023, 4, 1));
        PositionEntity testPositionToRemove = new PositionEntity(11L, "pos_to_remove", "Position to remove");
        PositionHistory positionHistory2 = new PositionHistory(22L, testPositionToRemove, LocalDate.of(2023, 5, 1));
        testAppUser.setPositionHistory(Set.of(positionHistory1, positionHistory2));
        testAppUser.setPosition(testPosition);

        UpdatePositionHistoryRequest positionHistory1Update = new UpdatePositionHistoryRequest(21L, "2023-04-01");
        UpdatePositionHistoryRequest positionHistory2Update = new UpdatePositionHistoryRequest(22L, null);
        List<UpdatePositionHistoryRequest> requests = List.of(positionHistory1Update, positionHistory2Update);

        Mockito.when(appUserRepository.findById(1L)).thenReturn(Optional.of(testAppUser));
        Mockito.when(appUserRepository.save(Mockito.any())).thenReturn(testAppUser);

        appUserService.updatePositionHistoryData(requests, 1L);

        assertEquals(1, testAppUser.getPositionHistory().size());
    }

    @Test
    void updatePositionHistoryDataShouldUpdateRecordDate() {
        AppUserEntity testAppUser = createTestAppUser("first", "last", "mail@mail.com");
        testAppUser.setAppUserId(1L);
        testAppUser.setRegistrationFinished(true);
        PositionEntity testPosition = new PositionEntity(10L, "test_pos", "Test Position");
        PositionEntity testPosition2 = new PositionEntity(11L, "another_test_pos", "Another Test Position");
        PositionHistory positionHistory1 = new PositionHistory(21L, testPosition, LocalDate.of(2023, 4, 1));
        PositionHistory positionHistory2 = new PositionHistory(22L, testPosition2, LocalDate.of(2023, 6, 1));
        testAppUser.setPositionHistory(Set.of(positionHistory1, positionHistory2));
        testAppUser.setPosition(testPosition2);

        UpdatePositionHistoryRequest positionHistory2Update = new UpdatePositionHistoryRequest(22L, "2023-05-01");
        List<UpdatePositionHistoryRequest> requests = List.of(positionHistory2Update);

        Mockito.when(appUserRepository.findById(1L)).thenReturn(Optional.of(testAppUser));
        Mockito.when(appUserRepository.save(Mockito.any())).thenReturn(testAppUser);

        appUserService.updatePositionHistoryData(requests, 1L);

        assertEquals(2, testAppUser.getPositionHistory().size());
        assertEquals(LocalDate.of(2023, 5, 1), testAppUser.getPositionHistory().stream().filter(p -> p.getPositionHistoryId() == 22L).findFirst().get().getStartDate());

    }

}
























