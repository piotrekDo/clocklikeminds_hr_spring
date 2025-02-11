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
import com.example.clocklike_portal.timeoff.on_saturday.HolidayOnSaturdayUserEntityRepository;
import com.example.clocklike_portal.timeoff_history.RequestHistoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.Page;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;

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

    @MockBean
    HolidayOnSaturdayRepository holidayOnSaturdayRepository;

    @MockBean
    HolidayOnSaturdayUserEntityRepository holidayOnSaturdayUserEntityRepository;
    @MockBean
    PtoTransformer ptoTransformer;
    @MockBean
    RequestHistoryRepository requestHistoryRepository;

    @MockBean
    EmailService emailService;

    @TestConfiguration
    static class AppUserServiceTestConfiguration {
        @Bean
        AppUserService appUserService(AppUserRepository appUserRepository, UserRoleRepository userRoleRepository, PositionRepository positionRepository,
                                      PositionHistoryRepository positionHistoryRepository, EmailService emailService, HolidayOnSaturdayRepository holidayOnSaturdayRepository,
                                      HolidayOnSaturdayUserEntityRepository holidayOnSaturdayUserEntityRepository, PtoTransformer ptoTransformer, RequestHistoryRepository requestHistoryRepository) {
            return new AppUserService(appUserRepository, requestHistoryRepository, userRoleRepository, positionRepository, positionHistoryRepository, emailService, holidayOnSaturdayRepository, holidayOnSaturdayUserEntityRepository, ptoTransformer);
        }
    }

    @Test
    void registerNewUserShouldCreateNewEntityAndSave() {
        GooglePrincipal googlePrincipal = new GooglePrincipal("test", "test", "www.images.com/image.img", "test@test.com", "pl", "clocklike.com");
        UserRole userRole = new UserRole("user");

        Mockito.when(userRoleRepository.findByRoleNameIgnoreCase("user")).thenReturn(Optional.of(userRole));
        Mockito.when(appUserRepository.save(Mockito.any())).thenAnswer(invocation -> invocation.getArgument(0));

        AppUserEntity result = appUserService.registerNewUser(googlePrincipal);
        assertEquals("test", result.getFirstName());
        assertEquals("test", result.getLastName());
        assertEquals("test@test.com", result.getUserEmail());
        assertFalse(result.isRegistrationFinished());
        assertEquals(1, result.getUserRoles().size());
        assertTrue(result.getUserRoles().contains(userRole));
    }

    @Test
    void finishRegistrationShouldThrowAnExceptionWhenUserNotFound() {
        FinishRegistrationRequest request = new FinishRegistrationRequest(12L, false, null, null, null, null, null, null);
        Mockito.when(appUserRepository.findById(12L)).thenReturn(Optional.empty());

        NoSuchElementException exception = assertThrows(NoSuchElementException.class, () -> appUserService.finishRegistration(request));
        assertTrue(exception.getMessage().startsWith("No user found"));
        Mockito.verify(userRoleRepository, Mockito.never()).save(Mockito.any());
    }

    @Test
    void finishRegistrationShouldThrowAnExceptionAndNotSaveChangesIfRegistrationWasFinishedBefore() {
        FinishRegistrationRequest request = new FinishRegistrationRequest(12L, false, null, null, null, null, null, null);
        AppUserEntity testAppUser = createTestAppUser("test", "test", "test@test.com");
        testAppUser.setRegistrationFinished(true);

        Mockito.when(appUserRepository.findById(12L)).thenReturn(Optional.of(testAppUser));

        IllegalOperationException exception = assertThrows(IllegalOperationException.class, () -> appUserService.finishRegistration(request));
        assertTrue(exception.getMessage().startsWith("User test@test.com registration is already finished"));
        Mockito.verify(userRoleRepository, Mockito.never()).save(Mockito.any());
    }

    @Test
    void finishRegistrationShouldThrowAnExceptionWhenNoSupervisorEntityWasFound() {
        FinishRegistrationRequest request = new FinishRegistrationRequest(12L, false, "junior_java_dev", "2023-10-10", null, null, null, 13L);
        AppUserEntity testAppUser = createTestAppUser("test", "test", "test@test.com");

        Mockito.when(appUserRepository.findById(12L)).thenReturn(Optional.of(testAppUser));
        Mockito.when(appUserRepository.findById(13L)).thenReturn(Optional.empty());

        NoSuchElementException exception = assertThrows(NoSuchElementException.class, () -> appUserService.finishRegistration(request));
        assertTrue(exception.getMessage().startsWith("No such supervisor found"));
        Mockito.verify(userRoleRepository, Mockito.never()).save(Mockito.any());
    }

    @Test
    void finishRegistrationShouldUpdateEntitySetSupervisorAndAddSubordinateToSupervisorAndSave() {
        FinishRegistrationRequest request = new FinishRegistrationRequest(12L, false, "junior_java_dev", "2023-10-10", null, 20, true, 13L);
        AppUserEntity testAppUser = createTestAppUser("test", "test", "test@test.com");
        AppUserEntity supervisor = createTestAppUser("supervisor", "supervisor", "supervisor@test.com");
        supervisor.setAppUserId(13L);
        supervisor.setSubordinates(new LinkedHashSet<>());
        PositionEntity juniorJavaDevPosition = new PositionEntity("junior_java_dev", "Junior Java Developer");

        Mockito.when(appUserRepository.findById(12L)).thenReturn(Optional.of(testAppUser));
        Mockito.when(appUserRepository.findById(13L)).thenReturn(Optional.of(supervisor));
        Mockito.when(positionRepository.findByPositionKeyIgnoreCase("junior_java_dev")).thenReturn(Optional.of(juniorJavaDevPosition));
        Mockito.when(appUserRepository.save(Mockito.any())).thenAnswer(invocation -> invocation.getArgument(0));

        appUserService.finishRegistration(request);
        assertEquals(testAppUser.getPosition(), juniorJavaDevPosition);
        assertEquals(supervisor, testAppUser.getSupervisor());
        assertTrue(supervisor.getSubordinates().contains(testAppUser));
        assertTrue(testAppUser.isRegistrationFinished());
        assertTrue(testAppUser.isActive());
        assertEquals(LocalDate.of(2023, 10, 10), testAppUser.getHireStart());
        assertNull(testAppUser.getHireEnd());
        assertEquals(1, testAppUser.getPositionHistory().size());
        assertEquals(20, testAppUser.getPtoDaysAccruedCurrentYear());
        assertEquals(20, testAppUser.getPtoDaysLeftCurrentYear());
    }

    @Test
    void findAllUsersShouldReturnAnEmptyListWhenNoUsersFoundAndNotThrowAnException() {
        assertEquals(Page.empty(), appUserService.findAllUsers(0, 10));
    }

    @Test
    void updateHireDataShouldThrowAnExceptionWhenNoUserFoundToUpdate() {
        UpdateHireDataRequest request = new UpdateHireDataRequest(1L, null, null, null, null, null, null);

        Mockito.when(appUserRepository.findById(1L)).thenReturn(Optional.empty());

        NoSuchElementException exception = assertThrows(NoSuchElementException.class, () -> appUserService.updateHireData(request));
        assertTrue(exception.getMessage().startsWith("No user found with id"));
        Mockito.verify(appUserRepository, Mockito.never()).save(Mockito.any());
    }

    @Test
    void updateHireDataShouldThrowAnExceptionWhenRegistrationOfUserToUpdateIsNotFinished() {
        UpdateHireDataRequest request = new UpdateHireDataRequest(1L, null, null, null, null, null, null);
        AppUserEntity testAppUser = createTestAppUser("test", "test", "test@test.com");

        Mockito.when(appUserRepository.findById(1L)).thenReturn(Optional.of(testAppUser));

        IllegalOperationException exception = assertThrows(IllegalOperationException.class, () -> appUserService.updateHireData(request));
        assertTrue(exception.getMessage().startsWith("User test@test.com is not active, finish registration first"));
        Mockito.verify(appUserRepository, Mockito.never()).save(Mockito.any());
    }

    @Test
    void updateHireDataShouldThrowAnExceptionWhenSelectedNewSupervisorDoesntHaveCorrespondingRole() {
        UpdateHireDataRequest request = new UpdateHireDataRequest(1L, null, null, null, null, null, 13L);
        UserRole supervisorRole = new UserRole("supervisor");
        AppUserEntity previousSupervisor = createTestAppUser("previousSupervisor", "previousSupervisor", "previousSupervisor@test.com");
        previousSupervisor.setAppUserId(22L);
        AppUserEntity newSupervisor = createTestAppUser("newSupervisor", "newSupervisor", "newSupervisor@test.com");
        AppUserEntity testAppUser = createTestAppUser("test", "test", "test@test.com");
        testAppUser.setRegistrationFinished(true);
        testAppUser.setSupervisor(previousSupervisor);

        Mockito.when(appUserRepository.findById(1L)).thenReturn(Optional.of(testAppUser));
        Mockito.when(appUserRepository.findById(13L)).thenReturn(Optional.of(newSupervisor));
        Mockito.when(userRoleRepository.findByRoleNameIgnoreCase("supervisor")).thenReturn(Optional.of(supervisorRole));

        IllegalOperationException exception = assertThrows(IllegalOperationException.class, () -> appUserService.updateHireData(request));
        assertTrue(exception.getMessage().startsWith("Selected new supervisor doesn't have supervisor role"));
        Mockito.verify(appUserRepository, Mockito.never()).save(Mockito.any());
    }

    @Test
    void updateHireDataShouldChangeSupervisorAndTransferAllPendingPtoRequestsToNewSupervisor() {
        UpdateHireDataRequest request = new UpdateHireDataRequest(1L, null, null, null, null, null, 13L);
        UserRole supervisorRole = new UserRole("supervisor");
        AppUserEntity previousSupervisor = createTestAppUser("previousSupervisor", "previousSupervisor", "previousSupervisor@test.com");
        previousSupervisor.setAppUserId(12L);
        previousSupervisor.setPtoAcceptor(new LinkedHashSet<>());
        AppUserEntity newSupervisor = createTestAppUser("newSupervisor", "newSupervisor", "newSupervisor@test.com");
        newSupervisor.setAppUserId(13L);
        newSupervisor.setUserRoles(Set.of(supervisorRole));
        newSupervisor.setPtoAcceptor(new LinkedHashSet<>());
        newSupervisor.setPtoRequests(new LinkedHashSet<>());
        AppUserEntity testAppUser = createTestAppUser("test", "test", "test@test.com");
        testAppUser.setAppUserId(1L);
        testAppUser.setRegistrationFinished(true);
        testAppUser.setSupervisor(previousSupervisor);
        testAppUser.setPtoRequests(new LinkedHashSet<>());
        PtoEntity ptoNoDecision = new PtoEntity(99L, "", false, null, LocalDateTime.of(2023, 5, 5, 12, 0).atOffset(ZoneOffset.UTC), LocalDate.of(2023, 5, 5), LocalDate.of(2023, 5, 6), testAppUser, previousSupervisor, false, null, 2, 0, false, false,  null, new ArrayList<>(), "");
        PtoEntity ptoAccepted = new PtoEntity(98L, "", false, null, LocalDateTime.of(2023, 5, 6, 12, 0).atOffset(ZoneOffset.UTC), LocalDate.of(2023, 5, 6), LocalDate.of(2023, 5, 7), testAppUser, previousSupervisor, true, LocalDateTime.of(2023, 5, 6, 13, 0).atOffset(ZoneOffset.UTC), 2, 0,  false, false, null, new ArrayList<>(), "");
        PtoEntity ptoRejected = new PtoEntity(97L, "", false, null, LocalDateTime.of(2023, 10, 10, 12, 0).atOffset(ZoneOffset.UTC), LocalDate.of(2023, 10, 11), LocalDate.of(2023, 10, 11), testAppUser, previousSupervisor, false, LocalDateTime.of(2023, 10, 11, 13, 0).atOffset(ZoneOffset.UTC), 1, 0, false, false, null, new ArrayList<>(), "");
        PtoEntity anotherPto = new PtoEntity(96L, "", false, null, LocalDateTime.of(2023, 2, 2, 12, 0).atOffset(ZoneOffset.UTC), LocalDate.of(2023, 2, 2), LocalDate.of(2023, 2, 2), newSupervisor, newSupervisor, false, null, 1, 1,  false, false, null, new ArrayList<>(), "");
        testAppUser.getPtoRequests().addAll(Set.of(ptoNoDecision, ptoAccepted, ptoRejected));
        previousSupervisor.getPtoAcceptor().addAll(Set.of(ptoNoDecision, ptoAccepted, ptoRejected));
        newSupervisor.getPtoRequests().add(anotherPto);
        newSupervisor.getPtoAcceptor().add(anotherPto);

        Mockito.when(appUserRepository.findById(1L)).thenReturn(Optional.of(testAppUser));
        Mockito.when(appUserRepository.findById(12L)).thenReturn(Optional.of(previousSupervisor));
        Mockito.when(appUserRepository.findById(13L)).thenReturn(Optional.of(newSupervisor));
        Mockito.when(userRoleRepository.findByRoleNameIgnoreCase("supervisor")).thenReturn(Optional.of(supervisorRole));
        Mockito.when(appUserRepository.save(testAppUser)).thenReturn(testAppUser);

        appUserService.updateHireData(request);

        assertEquals(newSupervisor, testAppUser.getSupervisor());
        assertTrue(newSupervisor.getSubordinates().contains(testAppUser));
        assertFalse(previousSupervisor.getSubordinates().contains(testAppUser));
        assertEquals(Set.of(ptoNoDecision, ptoAccepted, ptoRejected), testAppUser.getPtoRequests());
        assertEquals(Set.of(ptoAccepted, ptoRejected), previousSupervisor.getPtoAcceptor());
        assertEquals(Set.of(ptoNoDecision, anotherPto), newSupervisor.getPtoAcceptor());
    }

    @Test
    void updateHolidayDataShouldThrowAnExceptionWhenNoUserFoundToUpdate() {
        UpdateEmployeeHolidayDataRequest request = new UpdateEmployeeHolidayDataRequest(1L, null, null);

        Mockito.when(appUserRepository.findById(1L)).thenReturn(Optional.empty());

        NoSuchElementException exception = assertThrows(NoSuchElementException.class, () -> appUserService.updateHolidayData(request));
        assertTrue(exception.getMessage().startsWith("No user found with id: 1"));
        Mockito.verify(appUserRepository, Mockito.never()).save(Mockito.any());
    }

    @Test
    void updateHolidayDataShouldThrowAnExceptionWhenUserToUpdateHasUnfinishedRegistration() {
        UpdateEmployeeHolidayDataRequest request = new UpdateEmployeeHolidayDataRequest(1L, null, null);
        AppUserEntity testAppUser = createTestAppUser("test", "test", "test@test.com");

        Mockito.when(appUserRepository.findById(1L)).thenReturn(Optional.of(testAppUser));

        IllegalOperationException exception = assertThrows(IllegalOperationException.class, () -> appUserService.updateHolidayData(request));
        assertTrue(exception.getMessage().startsWith("User test@test.com is not active, finish registration first"));
        Mockito.verify(appUserRepository, Mockito.never()).save(Mockito.any());
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

        AppUserDto dto = new AppUserDto(1L, false, "first", "last", "mail@mail.com", null, null, true, true, true, null, null, null, null, 0L, parseInt(lastYearReqVal), parseInt(totalDaysNewReqVal), parseInt(userLastYearDaysLeft), parseInt(userCurrYearDaysLeft), parseInt(daysTaken), 0, null, null, null);

        Mockito.when(appUserRepository.findById(1L)).thenReturn(Optional.of(testAppUser));
        Mockito.when(appUserRepository.save(Mockito.any())).thenReturn(testAppUser);

        appUserService.updateHolidayData(request);

        assertEquals(parseInt(userCurrYearDaysLeft), testAppUser.getPtoDaysLeftCurrentYear());
        assertEquals(parseInt(userLastYearDaysLeft), testAppUser.getPtoDaysLeftFromLastYear());
    }

    @Test
    void updatePositionHistoryDataShouldThrowAnExceptionWhenNoUserFoundToUpdate() {
        Mockito.when(appUserRepository.findById(1L)).thenReturn(Optional.empty());

        NoSuchElementException exception = assertThrows(NoSuchElementException.class, () -> appUserService.updatePositionHistoryData(new ArrayList<>(), 1L));
        assertTrue(exception.getMessage().startsWith("No user found with id: 1"));
        Mockito.verify(appUserRepository, Mockito.never()).save(Mockito.any());
    }

    @Test
    void updatePositionHistoryDataShouldThrowAnExceptionWhenUserToUpdateHasUnfinishedRegistration() {
        AppUserEntity testAppUser = createTestAppUser("test", "test", "test@test.com");
        Mockito.when(appUserRepository.findById(1L)).thenReturn(Optional.of(testAppUser));

        IllegalOperationException exception = assertThrows(IllegalOperationException.class, () -> appUserService.updatePositionHistoryData(new ArrayList<>(), 1L));
        assertTrue(exception.getMessage().startsWith("User test@test.com is not active, finish registration first"));
        Mockito.verify(appUserRepository, Mockito.never()).save(Mockito.any());
    }

    @Test
    void updatePositionHistoryDataShouldThrowAnExceptionWhenTryingToRemoveCurrentJobPosition() {
        UpdatePositionHistoryRequest updatePositionHistoryRequest = new UpdatePositionHistoryRequest(12L, null);
        PositionEntity juniorJavaDevPositionEntity = new PositionEntity(21L, "junior_java_dev", "Junior Java Developer");
        AppUserEntity testAppUser = createTestAppUser("test", "test", "test@test.com");
        testAppUser.setAppUserId(1L);
        testAppUser.setRegistrationFinished(true);
        testAppUser.setPosition(juniorJavaDevPositionEntity);
        testAppUser.setPositionHistory(Set.of(new PositionHistory(12L, juniorJavaDevPositionEntity, LocalDate.of(2023, 1, 26))));

        Mockito.when(appUserRepository.findById(1L)).thenReturn(Optional.of(testAppUser));

        IllegalOperationException exception = assertThrows(IllegalOperationException.class, () -> appUserService.updatePositionHistoryData(List.of(updatePositionHistoryRequest), 1L));
        assertEquals("Cannot remove current position", exception.getMessage());
        Mockito.verify(appUserRepository, Mockito.never()).save(Mockito.any());
    }

    @Test
    void updatePositionHistoryDataShouldThrowAnExceptionWhenRequestPositionNotFoundAtUsersPositions() {
        UpdatePositionHistoryRequest updatePositionHistoryRequest = new UpdatePositionHistoryRequest(99L, null);
        PositionEntity juniorJavaDevPositionEntity = new PositionEntity(21L, "junior_java_dev", "Junior Java Developer");
        AppUserEntity testAppUser = createTestAppUser("test", "test", "test@test.com");
        testAppUser.setAppUserId(1L);
        testAppUser.setRegistrationFinished(true);
        testAppUser.setPosition(juniorJavaDevPositionEntity);
        testAppUser.setPositionHistory(Set.of(new PositionHistory(12L, juniorJavaDevPositionEntity, LocalDate.of(2023, 1, 26))));

        Mockito.when(appUserRepository.findById(1L)).thenReturn(Optional.of(testAppUser));

        IllegalOperationException exception = assertThrows(IllegalOperationException.class, () -> appUserService.updatePositionHistoryData(List.of(updatePositionHistoryRequest), 1L));
        assertEquals("No such position found at user's history", exception.getMessage());
        Mockito.verify(appUserRepository, Mockito.never()).save(Mockito.any());
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

    @Test
    void updateUserPermissionShouldThrowAnExceptionIfNoUserFoundTUpdate() {
        UpdateUserPermissionRequest request = new UpdateUserPermissionRequest(1L, null, null, null);
        Mockito.when(appUserRepository.findById(1L)).thenReturn(Optional.empty());

        NoSuchElementException exception = assertThrows(NoSuchElementException.class, () -> appUserService.updateUserPermission(request));
        assertEquals("No user found with id: 1", exception.getMessage());
        Mockito.verify(appUserRepository, Mockito.never()).save(Mockito.any());
    }

    @Test
    void updateUserPermissionShouldThrowAnExceptionWhenUserToUpdateHasUnfinishedRegistration() {
        UpdateUserPermissionRequest request = new UpdateUserPermissionRequest(1L, null, null, null);
        AppUserEntity testAppUser = createTestAppUser("test", "test", "test@test.com");
        Mockito.when(appUserRepository.findById(1L)).thenReturn(Optional.of(testAppUser));

        IllegalOperationException exception = assertThrows(IllegalOperationException.class, () -> appUserService.updateUserPermission(request));
        assertEquals("User test@test.com is not active, finish registration first", exception.getMessage());
        Mockito.verify(appUserRepository, Mockito.never()).save(Mockito.any());
    }

    @Test
    void updateUserPermissionShouldSetUserToInactiveAndRemoveAdminRole() {
        UpdateUserPermissionRequest request = new UpdateUserPermissionRequest(1L, false, null, false);
        UserRole adminRole = new UserRole("admin");
        AppUserEntity testAppUser = createTestAppUser("test", "test", "test@test.com");
        testAppUser.setAppUserId(1L);
        testAppUser.setRegistrationFinished(true);
        testAppUser.setActive(true);
        testAppUser.setUserRoles(new ArrayList<>());
        testAppUser.getUserRoles().add(adminRole);

        Mockito.when(appUserRepository.findById(1L)).thenReturn(Optional.of(testAppUser));
        Mockito.when(userRoleRepository.findByRoleNameIgnoreCase("admin")).thenReturn(Optional.of(adminRole));
        Mockito.when(appUserRepository.save(testAppUser)).thenReturn(testAppUser);
        appUserService.updateUserPermission(request);

        assertFalse(testAppUser.isActive());
        assertFalse(testAppUser.getUserRoles().contains(adminRole));
    }

    @Test
    void updateUserPermissionShouldRemoveSupervisorRoleAndDeclineAllPendingPtoRequestReturningReservedPtoDays() {
        UpdateUserPermissionRequest request = new UpdateUserPermissionRequest(1L, null, false, null);
        UserRole supervisorRole = new UserRole("supervisor");
        AppUserEntity testAppUser = createTestAppUser("supervisor", "supervisor", "supervisor@test.com");
        testAppUser.setAppUserId(1L);
        testAppUser.setRegistrationFinished(true);
        testAppUser.setActive(true);
        testAppUser.getUserRoles().add(supervisorRole);
        AppUserEntity employee1 = createTestAppUser("employee", "employee", "employee@mail.com");
        employee1.setAppUserId(2L);
        employee1.setSupervisor(testAppUser);
        testAppUser.getSubordinates().add(employee1);
        AppUserEntity employee2 = createTestAppUser("employee2", "employee2", "employee2@mail.com");
        employee2.setAppUserId(3L);
        employee2.setSupervisor(testAppUser);
        testAppUser.getSubordinates().add(employee2);
        PtoEntity emp1PendingPto = new PtoEntity(false, null, null, employee1, testAppUser, 10, 5);
        emp1PendingPto.setPtoRequestId(99L);
        employee1.getPtoRequests().add(emp1PendingPto);
        testAppUser.getPtoAcceptor().add(emp1PendingPto);
        PtoEntity emp1AcceptedPto = new PtoEntity(false, null, null, employee1, testAppUser, 2, 0);
        emp1AcceptedPto.setPtoRequestId(98L);
        emp1AcceptedPto.setWasAccepted(true);
        emp1AcceptedPto.setDecisionDateTime(OffsetDateTime.now(ZoneOffset.UTC));
        employee1.getPtoRequests().add(emp1AcceptedPto);
        testAppUser.getPtoAcceptor().add(emp1AcceptedPto);
        PtoEntity emp2PendingPto = new PtoEntity(false, null, null, employee2, testAppUser, 10, 1);
        emp2PendingPto.setPtoRequestId(97L);
        employee2.getPtoRequests().add(emp2PendingPto);
        testAppUser.getPtoAcceptor().add(emp2PendingPto);
        PtoEntity emp2DeclinedPto = new PtoEntity(false, null, null, employee2, testAppUser, 2, 0);
        emp2DeclinedPto.setPtoRequestId(96L);
        emp2DeclinedPto.setWasAccepted(false);
        emp2DeclinedPto.setDecisionDateTime(OffsetDateTime.now(ZoneOffset.UTC));
        employee2.getPtoRequests().add(emp2DeclinedPto);
        testAppUser.getPtoAcceptor().add(emp2DeclinedPto);

        Mockito.when(userRoleRepository.findByRoleNameIgnoreCase("supervisor")).thenReturn(Optional.of(supervisorRole));
        Mockito.when(appUserRepository.findById(1L)).thenReturn(Optional.of(testAppUser));
        Mockito.when(appUserRepository.save(testAppUser)).thenReturn(testAppUser);

        appUserService.updateUserPermission(request);

        assertFalse(testAppUser.getUserRoles().contains(supervisorRole));
        assertEquals(0, testAppUser.getSubordinates().size());
        assertEquals(4, testAppUser.getPtoAcceptor().size());
        assertNull(employee1.getSupervisor());
        assertNull(employee2.getSupervisor());
        assertEquals(5, employee1.getPtoDaysLeftCurrentYear());
        assertEquals(5, employee1.getPtoDaysLeftFromLastYear());
        assertEquals(9, employee2.getPtoDaysLeftCurrentYear());
        assertEquals(1, employee2.getPtoDaysLeftFromLastYear());
        assertFalse(emp1PendingPto.isWasAccepted());
        assertNotNull(emp1PendingPto.getDecisionDateTime());
        assertFalse(emp2PendingPto.isWasAccepted());
        assertNotNull(emp2PendingPto.getDecisionDateTime());
    }

}
