package com.example.clocklike_portal.pto;

import com.example.clocklike_portal.appUser.AppUserEntity;
import com.example.clocklike_portal.appUser.AppUserRepository;
import com.example.clocklike_portal.appUser.UserRole;
import com.example.clocklike_portal.dates_calculations.DateChecker;
import com.example.clocklike_portal.dates_calculations.HolidayService;
import com.example.clocklike_portal.error.IllegalOperationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static com.example.clocklike_portal.appUser.AppUserEntity.createTestAppUser;
import static java.lang.Integer.parseInt;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
class PtoServiceTest {

    @Autowired
    PtoService ptoService;
    @MockBean
    PtoRepository ptoRepository;
    @MockBean
    OccasionalLeaveTypeRepository occasionalLeaveTypeRepository;
    @MockBean
    AppUserRepository appUserRepository;
    @MockBean
    PtoTransformer ptoTransformer;
    @MockBean
    HolidayService holidayService;
    @MockBean
    DateChecker dateChecker;
    @MockBean
    HolidayOnSaturdayRepository holidayOnSaturdayRepository;
    @MockBean
    HolidayOnSaturdayUserEntityRepository holidayOnSaturdayUserEntityRepository;


    @TestConfiguration
    static class PtoServiceTestConfiguration {
        @Bean
        PtoService ptoService(PtoRepository ptoRepository, AppUserRepository appUserRepository, PtoTransformer ptoTransformer,
                              HolidayService holidayService, DateChecker dateChecker, OccasionalLeaveTypeRepository occasionalLeaveTypeRepository,
                              HolidayOnSaturdayRepository holidayOnSaturdayRepository, HolidayOnSaturdayUserEntityRepository holidayOnSaturdayUserEntityRepository) {
            return new PtoService(ptoRepository, appUserRepository, ptoTransformer, holidayService, dateChecker, occasionalLeaveTypeRepository,
                    holidayOnSaturdayRepository, holidayOnSaturdayUserEntityRepository);
        }
    }

    @Test
    void resolveNewRequestShouldThrowAnExceptionIfNoApplierUserFound() {
        NewPtoRequest request = new NewPtoRequest(null, null, 1L, 2L, "", null, null);
        Mockito.when(appUserRepository.findById(1L)).thenReturn(Optional.empty());

        NoSuchElementException exception = assertThrows(NoSuchElementException.class, () -> ptoService.processNewRequest(request));
        assertEquals("No user found for applier with ID: 1", exception.getMessage());
        Mockito.verify(appUserRepository, Mockito.never()).save(Mockito.any());
    }

    @Test
    void resolveNewRequestShouldThrowAnExceptionIfNoAcceptorUserFound() {
        NewPtoRequest request = new NewPtoRequest(null, null, 1L, 2L, "", null, null);
        AppUserEntity applier = createTestAppUser("test", "test", "test@test.com");
        applier.setAppUserId(1L);
        applier.setActive(true);

        Mockito.when(appUserRepository.findById(1L)).thenReturn(Optional.of(applier));
        Mockito.when(appUserRepository.findById(2L)).thenReturn(Optional.empty());

        NoSuchElementException exception = assertThrows(NoSuchElementException.class, () -> ptoService.processNewRequest(request));
        assertEquals("No user found for acceptor with ID: 2", exception.getMessage());
        Mockito.verify(appUserRepository, Mockito.never()).save(Mockito.any());
    }

    @Test
    void resolveNewRequestShouldThrowAnExceptionIfApplierNotActive() {
        NewPtoRequest request = new NewPtoRequest(null, null, 1L, 2L, "", null, null);
        AppUserEntity applier = createTestAppUser("test", "test", "test@test.com");
        applier.setAppUserId(1L);

        Mockito.when(appUserRepository.findById(1L)).thenReturn(Optional.of(applier));

        IllegalOperationException exception = assertThrows(IllegalOperationException.class, () -> ptoService.processNewRequest(request));
        assertEquals("Applier account is not active.", exception.getMessage());
        Mockito.verify(appUserRepository, Mockito.never()).save(Mockito.any());
    }

    @Test
    void resolveNewRequestShouldThrowAnExceptionIfAcceptorNotActive() {
        NewPtoRequest request = new NewPtoRequest(null, null, 1L, 2L, "", null, null);
        AppUserEntity applier = createTestAppUser("test", "test", "test@test.com");
        applier.setAppUserId(1L);
        applier.setActive(true);
        AppUserEntity acceptor = createTestAppUser("acceptor", "acceptor", "acceptor@mail.com");
        acceptor.setAppUserId(2L);

        Mockito.when(appUserRepository.findById(1L)).thenReturn(Optional.of(applier));
        Mockito.when(appUserRepository.findById(2L)).thenReturn(Optional.of(acceptor));

        IllegalOperationException exception = assertThrows(IllegalOperationException.class, () -> ptoService.processNewRequest(request));
        assertEquals("Acceptor account is not active.", exception.getMessage());
        Mockito.verify(appUserRepository, Mockito.never()).save(Mockito.any());
    }

    @Test
    void resolveNewRequestShouldThrowAnExceptionIfAcceptorDoesNotHaveEitherRoleOfAdminOrSupervisor() {
        NewPtoRequest request = new NewPtoRequest(null, null, 1L, 2L, "", null, null);
        AppUserEntity applier = createTestAppUser("test", "test", "test@test.com");
        applier.setAppUserId(1L);
        applier.setActive(true);
        AppUserEntity acceptor = createTestAppUser("acceptor", "acceptor", "acceptor@mail.com");
        acceptor.setAppUserId(2L);
        acceptor.setActive(true);

        Mockito.when(appUserRepository.findById(1L)).thenReturn(Optional.of(applier));
        Mockito.when(appUserRepository.findById(2L)).thenReturn(Optional.of(acceptor));

        IllegalOperationException exception = assertThrows(IllegalOperationException.class, () -> ptoService.processNewRequest(request));
        assertEquals("Selected acceptor has no authorities to accept pto requests", exception.getMessage());
        Mockito.verify(appUserRepository, Mockito.never()).save(Mockito.any());
    }

    @Test
    void resolveNewRequestShouldThrowAnExceptionIfPtoEndDateIsBeforeStartDate() {
        UserRole supervisorRole = new UserRole("supervisor");
        NewPtoRequest request = new NewPtoRequest("2023-05-01", "2023-04-30", 1L, 2L, "", null, null);
        AppUserEntity applier = createTestAppUser("test", "test", "test@test.com");
        applier.setAppUserId(1L);
        applier.setActive(true);
        AppUserEntity acceptor = createTestAppUser("acceptor", "acceptor", "acceptor@mail.com");
        acceptor.setAppUserId(2L);
        acceptor.setActive(true);
        acceptor.getUserRoles().add(supervisorRole);

        Mockito.when(appUserRepository.findById(1L)).thenReturn(Optional.of(applier));
        Mockito.when(appUserRepository.findById(2L)).thenReturn(Optional.of(acceptor));
        Mockito.when(dateChecker.checkIfDatesRangeIsValid(LocalDate.of(2023, 5, 1), LocalDate.of(2023, 4, 30))).thenReturn(false);

        IllegalOperationException exception = assertThrows(IllegalOperationException.class, () -> ptoService.processNewRequest(request));
        assertEquals("End date cannot be before start date", exception.getMessage());
        Mockito.verify(appUserRepository, Mockito.never()).save(Mockito.any());
    }

    @Test
    void resolveNewRequestShouldThrowAnExceptionIfAnotherRequestColliding() {
        UserRole supervisorRole = new UserRole("supervisor");
        NewPtoRequest request = new NewPtoRequest("2023-05-01", "2023-05-01", 1L, 2L, "", null, null);
        AppUserEntity applier = createTestAppUser("test", "test", "test@test.com");
        applier.setAppUserId(1L);
        applier.setActive(true);
        AppUserEntity acceptor = createTestAppUser("acceptor", "acceptor", "acceptor@mail.com");
        acceptor.setAppUserId(2L);
        acceptor.setActive(true);
        acceptor.getUserRoles().add(supervisorRole);

        Mockito.when(appUserRepository.findById(1L)).thenReturn(Optional.of(applier));
        Mockito.when(appUserRepository.findById(2L)).thenReturn(Optional.of(acceptor));
        Mockito.when(dateChecker.checkIfDatesRangeIsValid(LocalDate.of(2023, 5, 1), LocalDate.of(2023, 5, 1))).thenReturn(true);
        Mockito.when(ptoRepository.findAllOverlappingRequests(applier, LocalDate.of(2023, 5, 1), LocalDate.of(2023, 5, 1)))
                .thenReturn(List.of(new PtoEntity()));

        IllegalOperationException exception = assertThrows(IllegalOperationException.class, () -> ptoService.processNewRequest(request));
        assertEquals("Request colliding with other pto request", exception.getMessage());
        Mockito.verify(appUserRepository, Mockito.never()).save(Mockito.any());
    }

    @Test
    void resolveNewRequestShouldThrowAnExceptionWhenRequestTypeIsNull() {
        UserRole supervisorRole = new UserRole("supervisor");
        NewPtoRequest request = new NewPtoRequest("2023-05-01", "2023-05-01", 1L, 2L, null, null, null);
        AppUserEntity applier = createTestAppUser("test", "test", "test@test.com");
        applier.setAppUserId(1L);
        applier.setActive(true);
        AppUserEntity acceptor = createTestAppUser("acceptor", "acceptor", "acceptor@mail.com");
        acceptor.setAppUserId(2L);
        acceptor.setActive(true);
        acceptor.getUserRoles().add(supervisorRole);

        Mockito.when(appUserRepository.findById(1L)).thenReturn(Optional.of(applier));
        Mockito.when(appUserRepository.findById(2L)).thenReturn(Optional.of(acceptor));
        Mockito.when(dateChecker.checkIfDatesRangeIsValid(LocalDate.of(2023, 5, 1), LocalDate.of(2023, 5, 1))).thenReturn(true);
        Mockito.when(ptoRepository.findAllOverlappingRequests(applier, LocalDate.of(2023, 5, 1), LocalDate.of(2023, 5, 1)))
                .thenReturn(Collections.emptyList());

        IllegalOperationException exception = assertThrows(IllegalOperationException.class, () -> ptoService.processNewRequest(request));
        assertEquals("Request type not provided", exception.getMessage());
        Mockito.verify(appUserRepository, Mockito.never()).save(Mockito.any());
    }

    @Test
    void resolveNewRequestShouldThrowAnExceptionWhenUnknownRequestTypeProvided() {
        UserRole supervisorRole = new UserRole("supervisor");
        NewPtoRequest request = new NewPtoRequest("2023-05-01", "2023-05-01", 1L, 2L, "incorrect type", null, null);
        AppUserEntity applier = createTestAppUser("test", "test", "test@test.com");
        applier.setAppUserId(1L);
        applier.setActive(true);
        AppUserEntity acceptor = createTestAppUser("acceptor", "acceptor", "acceptor@mail.com");
        acceptor.setAppUserId(2L);
        acceptor.setActive(true);
        acceptor.getUserRoles().add(supervisorRole);

        Mockito.when(appUserRepository.findById(1L)).thenReturn(Optional.of(applier));
        Mockito.when(appUserRepository.findById(2L)).thenReturn(Optional.of(acceptor));
        Mockito.when(dateChecker.checkIfDatesRangeIsValid(LocalDate.of(2023, 5, 1), LocalDate.of(2023, 5, 1))).thenReturn(true);
        Mockito.when(ptoRepository.findAllOverlappingRequests(applier, LocalDate.of(2023, 5, 1), LocalDate.of(2023, 5, 1)))
                .thenReturn(Collections.emptyList());

        IllegalOperationException exception = assertThrows(IllegalOperationException.class, () -> ptoService.processNewRequest(request));
        assertEquals("Unknown request type", exception.getMessage());
        Mockito.verify(appUserRepository, Mockito.never()).save(Mockito.any());
    }

    @Test
    void requestPtoShouldSetDaysTakenAndDaysLeftValuesOnAcceptorEntityAndAddPtoRequestToAcceptorAndApplier() {
        NewPtoRequest request = new NewPtoRequest("2024-02-12", "2024-02-16", 2L, 1L, "pto", null, null);
        AppUserEntity applier = AppUserEntity.createTestAppUser("applier", "applier", "applier@test.com");
        applier.setActive(true);
        applier.setPtoDaysLeftFromLastYear(2);
        applier.setPtoDaysLeftCurrentYear(20);
        AppUserEntity acceptor = AppUserEntity.createTestAppUser("acceptor", "acceptor", "acceptor@test.com");
        acceptor.setActive(true);
        acceptor.setUserRoles(List.of(new UserRole(1L, "admin")));
        PtoEntity ptoEntity = new PtoEntity(1L, "", false, null, LocalDateTime.now(), LocalDate.of(2024, 2, 12), LocalDate.of(2024, 2, 16), applier, acceptor, false, null, 5, 2, null);
        PtoDto ptoDto = new PtoDto(1L, "", false, null, true, false, LocalDateTime.now(), LocalDate.of(2024, 2, 12), LocalDate.of(2024, 2, 16), 2L, "applier", "applier", "applier@test.com", 17, 5, null, 1L, "acceptor", "acceptor", "acceptor@mail.com", null, 5, 5, 2, null, null, null, null, null, null);
        Mockito.when(appUserRepository.findById(2L)).thenReturn(Optional.of(applier));
        Mockito.when(appUserRepository.findById(1L)).thenReturn(Optional.of(acceptor));
        Mockito.when(dateChecker.checkIfDatesRangeIsValid(LocalDate.of(2024, 2, 12), LocalDate.of(2024, 2, 16))).thenReturn(true);
        Mockito.when(holidayService.calculateBusinessDays(LocalDate.of(2024, 2, 12), LocalDate.of(2024, 2, 16))).thenReturn(5);
        Mockito.when(ptoTransformer.ptoEntityFromNewRequest("", false, null, LocalDate.of(2024, 2, 12), LocalDate.of(2024, 2, 16), applier, acceptor, 5, 2)).thenReturn(ptoEntity);
        Mockito.when(ptoTransformer.ptoEntityToDto(ptoEntity)).thenReturn(ptoDto);

        ptoService.processNewRequest(request);

        assertEquals(17, applier.getPtoDaysLeftCurrentYear());
        assertEquals(0, applier.getPtoDaysLeftFromLastYear());
        assertEquals(5, applier.getPtoDaysTaken());
        assertEquals(1, applier.getPtoRequests().size());
        assertEquals(1, acceptor.getPtoAcceptor().size());
        assertEquals(0, acceptor.getPtoRequests().size());
    }

    @Test
    void processOccasionalLeaveRequestShouldThrowAnExceptionWhenNoOccasionalTypeIsSpecified() {
        NewPtoRequest newPtoRequest = new NewPtoRequest(null, null, null, null, null, null, null);

        IllegalOperationException exception = assertThrows(IllegalOperationException.class, () -> ptoService.processOccasionalLeaveRequest(newPtoRequest, null, null, null, null));
        assertEquals("No occasional type specified", exception.getMessage());
        Mockito.verify(appUserRepository, Mockito.never()).save(Mockito.any());
    }


//    @ParameterizedTest
//    @CsvSource({
//            "0,0,1",
//            "5,0,6",
//            "0,5,6",
//    })
//    void resolveNewRequestShouldThrowAnExceptionIfApplierHasInsufficientPtoDaysLeft(String ptoCurrentYear, String ptoLeftLastYear, String businessDays) {
//        UserRole supervisorRole = new UserRole("supervisor");
//        NewPtoRequest request = new NewPtoRequest("2023-05-01", "2023-05-01", 1L, 2L, "");
//        AppUserEntity applier = createTestAppUser("test", "test", "test@test.com");
//        applier.setAppUserId(1L);
//        applier.setActive(true);
//        applier.setPtoDaysLeftCurrentYear(parseInt(ptoCurrentYear));
//        applier.setPtoDaysLeftCurrentYear(parseInt(ptoLeftLastYear));
//        AppUserEntity acceptor = createTestAppUser("acceptor", "acceptor", "acceptor@mail.com");
//        acceptor.setAppUserId(2L);
//        acceptor.setActive(true);
//        acceptor.getUserRoles().add(supervisorRole);
//
//        Mockito.when(appUserRepository.findById(1L)).thenReturn(Optional.of(applier));
//        Mockito.when(appUserRepository.findById(2L)).thenReturn(Optional.of(acceptor));
//        Mockito.when(dateChecker.checkIfDatesRangeIsValid(LocalDate.of(2023, 5, 1), LocalDate.of(2023, 5, 1))).thenReturn(true);
//        Mockito.when(holidayService.calculateBusinessDays(Mockito.any(), Mockito.any())).thenReturn(parseInt(businessDays));
//        Mockito.when(ptoRepository.findAllOverlappingRequests(applier, LocalDate.of(2023, 5, 1), LocalDate.of(2023, 5, 1)))
//                .thenReturn(Collections.emptyList());
//
//        IllegalOperationException exception = assertThrows(IllegalOperationException.class, () -> ptoService.resolveNewRequest(request));
//        assertEquals("Insufficient pto days left", exception.getMessage());
//        Mockito.verify(appUserRepository, Mockito.never()).save(Mockito.any());
//    }

//    @Test
//    void resolveRequestShouldThrowAnExceptionWhenNoPtoToResolveFound() {
//        ResolvePtoRequest request = new ResolvePtoRequest(12L, false, null);
//        Mockito.when(ptoRepository.findById(12L)).thenReturn(Optional.empty());
//
//        NoSuchElementException exception = assertThrows(NoSuchElementException.class, () -> ptoService.resolveRequest(request));
//        assertEquals("No such PTO request found", exception.getMessage());
//        Mockito.verify(ptoRepository, Mockito.never()).save(Mockito.any());
//    }
//
//    @ParameterizedTest
//    @CsvSource({
//            "false,false,mail@mail.com",
//            "false,true,mail@mai.com"
//    })
//    @WithMockUser(username = "acceptor@test.com")
//    void resolveRequestShouldThrowAnExceptionIfAcceptorIsNotAuthorizedToResolveRequest(boolean hasAdminRole, boolean hasSupervisorRole, String acceptorEmail) {
//        UserRole supervisorRole = new UserRole("supervisor");
//        UserRole adminRole = new UserRole("admin");
//        ResolvePtoRequest request = new ResolvePtoRequest(12L, false, null);
//        AppUserEntity applier = createTestAppUser("applier", "applier", "applier@mail.com");
//        AppUserEntity acceptor = createTestAppUser("acceptor", "acceptor", acceptorEmail);
//        acceptor.setActive(true);
//        if (hasAdminRole) acceptor.getUserRoles().add(adminRole);
//        if (hasSupervisorRole) acceptor.getUserRoles().add(supervisorRole);
//        PtoEntity ptoEntity = PtoEntity.builder().ptoRequestId(12L).applier(applier).acceptor(acceptor).build();
//
//        Mockito.when(ptoRepository.findById(12L)).thenReturn(Optional.of(ptoEntity));
//
//        IllegalOperationException exception = assertThrows(IllegalOperationException.class, () -> ptoService.resolveRequest(request));
//        assertEquals("You are not authorized to resolve this PTO request.", exception.getMessage());
//        Mockito.verify(ptoRepository, Mockito.never()).save(Mockito.any());
//    }
//
//
//    @ParameterizedTest()
//    @CsvSource({
//            "10,2,3,9,0",
//            "10,0,5,5,0",
//            "0,10,5,0,5"
//    })
//    @WithMockUser(username = "acceptor@test.com")
//    void declining_pto_request_should_restore_pto_days_and_set_pto_as_declined(String accruedCurrYear, String accruedLastYear, String daysTaken, String leftCurrYear, String leftLastYear) {
//        UserRole adminRole = new UserRole("admin");
//        ResolvePtoRequest resolveRequest = new ResolvePtoRequest(99L, false, "just because");
//        AppUserEntity applier = AppUserEntity.createTestAppUser("applier", "applier", "applier@test.com");
//        applier.setPtoDaysAccruedCurrentYear(parseInt(accruedCurrYear));
//        applier.setPtoDaysAccruedLastYear(parseInt(accruedLastYear));
//        applier.setPtoDaysTaken(parseInt(daysTaken));
//        applier.setPtoDaysLeftCurrentYear(parseInt(leftCurrYear));
//        applier.setPtoDaysLeftFromLastYear(parseInt(leftLastYear));
//        AppUserEntity acceptor = AppUserEntity.createTestAppUser("acceptor", "acceptor", "acceptor@test.com");
//        acceptor.setActive(true);
//        acceptor.getUserRoles().add(adminRole);
//        PtoEntity ptoEntityFound = new PtoEntity(99L, "", false, null, LocalDateTime.of(2024, 2, 1, 12, 12), LocalDate.of(2024, 2, 1), LocalDate.of(2024, 2, 3), applier, acceptor, false, null, parseInt(daysTaken), 1, null);
//        PtoEntity ptoEntityUpdated = new PtoEntity(99L, "", false, null, LocalDateTime.of(2024, 2, 1, 12, 12), LocalDate.of(2024, 2, 1), LocalDate.of(2024, 2, 3), applier, acceptor, false, LocalDateTime.now(), parseInt(daysTaken), 1, "just because");
//        Mockito.when(ptoRepository.findById(99L)).thenReturn(Optional.of(ptoEntityFound));
//        Mockito.when(ptoRepository.save(Mockito.any())).thenReturn(ptoEntityUpdated);
//
//        ptoService.resolveRequest(resolveRequest);
//
//        assertEquals(0, applier.getPtoDaysTaken());
//        assertEquals(parseInt(accruedCurrYear), applier.getPtoDaysLeftCurrentYear());
//        assertEquals(parseInt(accruedLastYear), applier.getPtoDaysLeftFromLastYear());
//        assertEquals(ptoEntityUpdated.getDeclineReason(), ptoEntityFound.getDeclineReason());
//    }

}