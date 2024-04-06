package com.example.clocklike_portal.pto;

import com.example.clocklike_portal.appUser.AppUserEntity;
import com.example.clocklike_portal.appUser.AppUserRepository;
import com.example.clocklike_portal.appUser.UserRole;
import com.example.clocklike_portal.dates_calculations.DateChecker;
import com.example.clocklike_portal.dates_calculations.HolidayService;
import com.example.clocklike_portal.error.IllegalOperationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
class PtoServiceTest {

    @Autowired
    PtoService ptoService;

    @MockBean
    PtoRepository ptoRepository;

    @MockBean
    AppUserRepository appUserRepository;

    @MockBean
    PtoTransformer ptoTransformer;

    @MockBean
    HolidayService holidayService;

    @MockBean
    DateChecker dateChecker;


    @TestConfiguration
    static class PtoServiceTestConfiguration {
        @Bean
        PtoService ptoService(PtoRepository ptoRepository, AppUserRepository appUserRepository, PtoTransformer ptoTransformer, HolidayService holidayService, DateChecker dateChecker) {
            return new PtoService(ptoRepository, appUserRepository, ptoTransformer, holidayService, dateChecker);
        }
    }

    @Test
    void pto_request_should_set_correct_values_on_requestor_entity() {
        NewPtoRequest request = new NewPtoRequest("2024-02-12", "2024-02-16", 2L, 1L);
        AppUserEntity applier = AppUserEntity.createTestAppUser("applier", "applier", "applier@test.com");
        applier.setActive(true);
        applier.setPtoDaysLeftFromLastYear(2);
        applier.setPtoDaysLeftCurrentYear(20);
        AppUserEntity acceptor = AppUserEntity.createTestAppUser("acceptor", "acceptor", "acceptor@test.com");
        acceptor.setUserRoles(List.of(new UserRole(1L, "admin")));
        PtoEntity ptoEntity = new PtoEntity(1L, LocalDateTime.now(), LocalDate.of(2024, 2, 12), LocalDate.of(2024, 2, 16), applier, acceptor, false, null, 5, 2, null);
        PtoDto ptoDto = new PtoDto(1L, true, false, LocalDateTime.now(), LocalDate.of(2024, 2, 12), LocalDate.of(2024, 2, 16), 2L, "applier", "applier", "applier@test.com", 17, 5, 1L, "acceptor", "acceptor", "acceptor@mail.com", null, 5, 5, 2, null);
        Mockito.when(appUserRepository.findById(2L)).thenReturn(Optional.of(applier));
        Mockito.when(appUserRepository.findById(1L)).thenReturn(Optional.of(acceptor));
        Mockito.when(dateChecker.checkIfDatesRangeIsValid(LocalDate.of(2024, 2, 12), LocalDate.of(2024, 2, 16))).thenReturn(true);
        Mockito.when(holidayService.calculateBusinessDays(LocalDate.of(2024, 2, 12), LocalDate.of(2024, 2, 16))).thenReturn(5);
        Mockito.when(ptoTransformer.ptoEntityFromNewRequest(LocalDate.of(2024, 2, 12), LocalDate.of(2024, 2, 16), applier, acceptor, 5, 2)).thenReturn(ptoEntity);
        Mockito.when(ptoTransformer.ptoEntityToDto(ptoEntity)).thenReturn(ptoDto);

        PtoDto result = ptoService.requestPto(request);

        assertEquals(17, applier.getPtoDaysLeftCurrentYear());
        assertEquals(0, applier.getPtoDaysLeftFromLastYear());
        assertEquals(5, applier.getPtoDaysTaken());
        assertEquals(1, applier.getPtoRequests().size());
        assertEquals(1, acceptor.getPtoAcceptor().size());
    }

    @Test
    void pto_request_should_throw_an_exception_when_there_is_no_pto_days_left() {
        NewPtoRequest request = new NewPtoRequest("2024-02-12", "2024-02-01", 2L, 1L);
        AppUserEntity applier = new AppUserEntity();
        AppUserEntity acceptor = new AppUserEntity();
        Mockito.when(appUserRepository.findById(2L)).thenReturn(Optional.of(applier));
        Mockito.when(appUserRepository.findById(1L)).thenReturn(Optional.of(acceptor));
        Mockito.when(dateChecker.checkIfDatesRangeIsValid(LocalDate.of(2024, 2, 12), LocalDate.of(2024, 2, 1))).thenReturn(false);

        assertThrows(IllegalOperationException.class, () -> ptoService.requestPto(request));
    }

    @Test
    @WithMockUser(username = "acceptor@test.com")
    void declining_pto_request_should_restore_pto_days_and_set_pto_as_declined() {
        ResolvePtoRequest resolveRequest = new ResolvePtoRequest(99L, false, "just because");
        AppUserEntity applier = AppUserEntity.createTestAppUser("applier", "applier", "applier@test.com");
        applier.setPtoDaysTaken(2);
        applier.setPtoDaysLeftCurrentYear(10);
        applier.setPtoDaysLeftFromLastYear(0);
        AppUserEntity acceptor = AppUserEntity.createTestAppUser("acceptor", "acceptor", "acceptor@test.com");
        PtoEntity ptoEntityFound = new PtoEntity(99L, LocalDateTime.of(2024, 2, 1, 12, 12), LocalDate.of(2024, 2, 1), LocalDate.of(2024, 2, 2), applier, acceptor, false, null, 2, 1, null);
        PtoEntity ptoEntityUpdated = new PtoEntity(99L, LocalDateTime.of(2024, 2, 1, 12, 12), LocalDate.of(2024, 2, 1), LocalDate.of(2024, 2, 2), applier, acceptor, false, LocalDateTime.now(), 2, 1, "just because");
        Mockito.when(ptoRepository.findById(99L)).thenReturn(Optional.of(ptoEntityFound));
        Mockito.when(ptoRepository.save(Mockito.any())).thenReturn(ptoEntityUpdated);

        ptoService.resolveRequest(resolveRequest);

        assertEquals(0, applier.getPtoDaysTaken());
        assertEquals(11, applier.getPtoDaysLeftCurrentYear());
        assertEquals(1, applier.getPtoDaysLeftFromLastYear());
        assertEquals(ptoEntityUpdated.getDeclineReason(), ptoEntityFound.getDeclineReason());
    }

}