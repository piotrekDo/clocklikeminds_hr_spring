package com.example.clocklike_portal.timeoff;

import com.example.clocklike_portal.appUser.AppUserEntity;
import com.example.clocklike_portal.timeoff.occasional.OccasionalLeaveEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(SpringExtension.class)
@DataJpaTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class PtoRepositoryTest {
    @Autowired
    PtoRepository ptoRepository;

    @Autowired
    TestEntityManager testEntityManager;

    @Test
    void findRequestsForGivenYearShouldReturnOnlyAcceptedOrPendingRequestsForGivenUser() {
        AppUserEntity applier = testEntityManager.persist(AppUserEntity.createTestAppUser("applier", "applier", "applier@mail.com"));
        AppUserEntity applier2 = testEntityManager.persist(AppUserEntity.createTestAppUser("applier2", "applier2", "applier2@mail.com"));
        PtoEntity matching1 = testEntityManager.persist(PtoEntity.builder()
                .applier(applier)
                .ptoStart(LocalDate.of(2024, 12, 17))
                .ptoEnd(LocalDate.of(2025, 1, 8))
                .build());
        PtoEntity matching2 = testEntityManager.persist(PtoEntity.builder()
                .applier(applier)
                .ptoStart(LocalDate.of(2023, 12, 17))
                .ptoEnd(LocalDate.of(2024, 1, 8))
                .build());
        PtoEntity matching3 = testEntityManager.persist(PtoEntity.builder()
                .applier(applier)
                .ptoStart(LocalDate.of(2024, 3, 17))
                .ptoEnd(LocalDate.of(2024, 4, 8))
                .build());
        PtoEntity unMatching1 = testEntityManager.persist(PtoEntity.builder()
                .applier(applier2)
                .ptoStart(LocalDate.of(2024, 12, 17))
                .ptoEnd(LocalDate.of(2025, 1, 8))
                .build());
        PtoEntity unMatching2 = testEntityManager.persist(PtoEntity.builder()
                .applier(applier)
                .ptoStart(LocalDate.of(2025, 1, 1))
                .ptoEnd(LocalDate.of(2025, 1, 8))
                .build());
        PtoEntity unMatching3 = testEntityManager.persist(PtoEntity.builder()
                .applier(applier)
                .ptoStart(LocalDate.of(2023, 1, 1))
                .ptoEnd(LocalDate.of(2023, 1, 8))
                .build());
        PtoEntity unMatching4 = testEntityManager.persist(PtoEntity.builder()
                .applier(applier)
                .ptoStart(LocalDate.of(2024, 5, 1))
                .ptoEnd(LocalDate.of(2024, 6, 8))
                .wasAccepted(false)
                .decisionDateTime(OffsetDateTime.now(ZoneOffset.UTC))
                .build());
        PtoEntity unMatching5 = testEntityManager.persist(PtoEntity.builder()
                .applier(applier)
                .ptoStart(LocalDate.of(2023, 12, 1))
                .ptoEnd(LocalDate.of(2024, 1, 8))
                .wasAccepted(false)
                .decisionDateTime(OffsetDateTime.now(ZoneOffset.UTC))
                .build());

        List<PtoEntity> result = ptoRepository.findRequestsForYear(2024, 1L);
        assertEquals(3, result.size());
        assertEquals(matching1.getPtoRequestId(), result.get(0).getPtoRequestId());
        assertEquals(matching2.getPtoRequestId(), result.get(1).getPtoRequestId());
        assertEquals(matching3.getPtoRequestId(), result.get(2).getPtoRequestId());
    }

    @Test
    void findUserRequestsForChildCareShouldReturnCorrespondingRequests() {
        AppUserEntity applier = testEntityManager.persist(AppUserEntity.createTestAppUser("applier", "applier", "applier@mail.com"));
        ChildCareLeaveEntity matching = testEntityManager.persist(new ChildCareLeaveEntity(LocalDate.now(), LocalDate.now(), applier, null, 1));
        OccasionalLeaveEntity unMatching1 = testEntityManager.persist(new OccasionalLeaveEntity(LocalDate.of(2023, 1, 1), LocalDate.of(2023, 1, 2), applier, null, 1, null));
        PtoEntity unMatching2 = testEntityManager.persist(PtoEntity.builder().applier(applier).isDemand(true).requestDateTime(LocalDateTime.of(2023, 1, 1, 12, 12).atOffset(ZoneOffset.UTC)).build());

        List<PtoEntity> result = ptoRepository.findUserRequestsForChildCare(applier.getAppUserId());
        assertEquals(1, result.size());
        assertEquals(matching.getPtoRequestId(), result.get(0).getPtoRequestId());
    }

    @Test
    void findUserRequestsFromCurrentYearShouldReturnCorrespondingRequests() {
        AppUserEntity applier = testEntityManager.persist(AppUserEntity.createTestAppUser("applier", "applier", "applier@mail.com"));
        PtoEntity matching = testEntityManager.persist(PtoEntity.builder().applier(applier).isDemand(true).requestDateTime(LocalDateTime.of(LocalDate.now().getYear(), 1, 1, 12, 12).atOffset(ZoneOffset.UTC)).build());
        PtoEntity unMatching = testEntityManager.persist(PtoEntity.builder().applier(applier).isDemand(true).requestDateTime(LocalDateTime.of(2023, 1, 1, 12, 12).atOffset(ZoneOffset.UTC)).build());

        List<PtoEntity> result = ptoRepository.findUserRequestsOnDemandFromCurrentYear(applier.getAppUserId());
        assertEquals(1, result.size());
        assertEquals(matching.getPtoRequestId(), result.get(0).getPtoRequestId());
    }

    @Test
    void find_requests_by_acceptor_and_time_frame_should_return_corresponding_requests() {
        AppUserEntity acceptor = testEntityManager.persist(AppUserEntity.createTestAppUser("acceptor", "acceptor", "acceptor@mail.com"));
        AppUserEntity acceptor2 = testEntityManager.persist(AppUserEntity.createTestAppUser("acceptor", "acceptor", "acceptor2@mail.com"));

        PtoEntity unMatching1 = testEntityManager.persist(PtoEntity.builder().acceptor(acceptor2).ptoStart(LocalDate.of(2024, 4, 10)).ptoEnd(LocalDate.of(2024, 4, 12)).build());
        PtoEntity unMatching2 = testEntityManager.persist(PtoEntity.builder().acceptor(acceptor2).ptoStart(LocalDate.of(2024, 7, 10)).ptoEnd(LocalDate.of(2024, 12, 12)).decisionDateTime(OffsetDateTime.now(ZoneOffset.UTC)).wasAccepted(true).build());
        PtoEntity unMatching3 = testEntityManager.persist(PtoEntity.builder().acceptor(acceptor).ptoStart(LocalDate.of(2023, 4, 30)).ptoEnd(LocalDate.of(2023, 5, 12)).build());
        PtoEntity unMatching4 = testEntityManager.persist(PtoEntity.builder().acceptor(acceptor).ptoStart(LocalDate.of(2023, 5, 1)).ptoEnd(LocalDate.of(2023, 5, 12)).build());

        PtoEntity matching1 = testEntityManager.persist(PtoEntity.builder().acceptor(acceptor).ptoStart(LocalDate.of(2024, 5, 1)).ptoEnd(LocalDate.of(2024, 5, 31)).build());
        PtoEntity matching2 = testEntityManager.persist(PtoEntity.builder().acceptor(acceptor).ptoStart(LocalDate.of(2024, 5, 5)).ptoEnd(LocalDate.of(2024, 5, 20)).build());
        PtoEntity matching3 = testEntityManager.persist(PtoEntity.builder().acceptor(acceptor).ptoStart(LocalDate.of(2024, 5, 12)).ptoEnd(LocalDate.of(2024, 7, 1)).build());
        PtoEntity matching4 = testEntityManager.persist(PtoEntity.builder().acceptor(acceptor).ptoStart(LocalDate.of(2024, 4, 17)).ptoEnd(LocalDate.of(2024, 5, 10)).build());
        PtoEntity matching5 = testEntityManager.persist(PtoEntity.builder().acceptor(acceptor).ptoStart(LocalDate.of(2024, 4, 25)).ptoEnd(LocalDate.of(2024, 7, 10)).build());

        LocalDate start = LocalDate.of(2024, 5, 1);
        LocalDate end = LocalDate.of(2024, 5, 31);

        List<PtoEntity> result = ptoRepository.findRequestsByAcceptorAndTimeFrame(1L, start, end);

        result.forEach(x -> System.out.println(x.getPtoStart()
        ));
        assertEquals(5, result.size());
        assertEquals(matching1.getPtoRequestId(), result.get(0).getPtoRequestId());
        assertEquals(matching2.getPtoRequestId(), result.get(1).getPtoRequestId());
        assertEquals(matching3.getPtoRequestId(), result.get(2).getPtoRequestId());
        assertEquals(matching4.getPtoRequestId(), result.get(3).getPtoRequestId());
        assertEquals(matching5.getPtoRequestId(), result.get(4).getPtoRequestId());
    }

    @Test
    void find_requests_for_year_should_return_requests_for_given_year_and_january_next_year_and_december_last_year() {
        //given
        int givenYear = 2024;
        PtoEntity givenYearMidYear = new PtoEntity("", false, LocalDate.of(2024, 5, 10), LocalDate.of(2024, 5, 12), null, null, 0, 0);
        PtoEntity givenYearDec = new PtoEntity("", false, LocalDate.of(2024, 12, 10), LocalDate.of(2024, 12, 12), null, null, 0, 0);
        PtoEntity givenYearDecToJan = new PtoEntity("", false, LocalDate.of(2024, 12, 26), LocalDate.of(2025, 1, 3), null, null, 0, 0);
        PtoEntity givenYearJanDecPrev = new PtoEntity("", false, LocalDate.of(2023, 12, 26), LocalDate.of(2024, 1, 3), null, null, 0, 0);
        PtoEntity prevYearDec = new PtoEntity("", false, LocalDate.of(2023, 11, 26), LocalDate.of(2023, 12, 7), null, null, 0, 0);
        PtoEntity nextYearJan = new PtoEntity("", false, LocalDate.of(2025, 1, 26), LocalDate.of(2025, 2, 7), null, null, 0, 0);
        PtoEntity nextYearMidYear = new PtoEntity("", false, LocalDate.of(2025, 7, 12), LocalDate.of(2025, 7, 17), null, null, 0, 0);
        PtoEntity prevYearMidYear = new PtoEntity("", false, LocalDate.of(2025, 6, 12), LocalDate.of(2025, 7, 17), null, null, 0, 0);
        testEntityManager.persist(givenYearMidYear);
        testEntityManager.persist(givenYearDec);
        testEntityManager.persist(givenYearDecToJan);
        testEntityManager.persist(givenYearJanDecPrev);
        testEntityManager.persist(prevYearDec);
        testEntityManager.persist(nextYearJan);
        testEntityManager.persist(nextYearMidYear);
        testEntityManager.persist(prevYearMidYear);

        //when
        List<PtoEntity> result = ptoRepository.findRequestsForYear(2024);

        //then
        assertEquals(List.of(givenYearMidYear, givenYearDec, givenYearDecToJan, givenYearJanDecPrev, prevYearDec, nextYearJan), result);
    }

    @Test
    void find_request_by_user_and_date_frames_should_return_corresponding_requests() {
        AppUserEntity testAppUser = AppUserEntity.createTestAppUser("test", "test", "test@test.com");
        AppUserEntity testAppUser2 = AppUserEntity.createTestAppUser("test2", "test2", "test2@test.com");
        LocalDate testingStart = LocalDate.of(2024, 1, 3);
        LocalDate testingEnd = LocalDate.of(2024, 1, 6);

        PtoEntity pto1 = PtoEntity.builder()
                .applier(testAppUser)
                .ptoStart(LocalDate.of(2024, 1, 1))
                .ptoEnd(LocalDate.of(2024, 1, 3))
                .wasAccepted(true)
                .decisionDateTime(OffsetDateTime.now(ZoneOffset.UTC))
                .build();
        PtoEntity pto2 = PtoEntity.builder()
                .applier(testAppUser)
                .ptoStart(LocalDate.of(2024, 1, 5))
                .ptoEnd(LocalDate.of(2024, 1, 7))
                .wasAccepted(true)
                .decisionDateTime(OffsetDateTime.now(ZoneOffset.UTC))
                .build();
        PtoEntity pto3 = PtoEntity.builder()
                .applier(testAppUser)
                .ptoStart(LocalDate.of(2023, 12, 12))
                .ptoEnd(LocalDate.of(2023, 12, 28))
                .build();
        PtoEntity pto4 = PtoEntity.builder()
                .applier(testAppUser)
                .ptoStart(LocalDate.of(2024, 1, 8))
                .ptoEnd(LocalDate.of(2024, 1, 12))
                .build();
        PtoEntity pto5 = PtoEntity.builder()
                .applier(testAppUser)
                .ptoStart(LocalDate.of(2024, 1, 1))
                .ptoEnd(LocalDate.of(2024, 1, 3))
                .build();
        PtoEntity pto6 = PtoEntity.builder()
                .applier(testAppUser2)
                .ptoStart(LocalDate.of(2024, 1, 1))
                .ptoEnd(LocalDate.of(2024, 1, 3))
                .wasAccepted(true)
                .decisionDateTime(OffsetDateTime.now(ZoneOffset.UTC))
                .build();
        testEntityManager.persist(testAppUser);
        testEntityManager.persist(testAppUser2);
        testEntityManager.persist(pto1);
        testEntityManager.persist(pto2);
        testEntityManager.persist(pto3);
        testEntityManager.persist(pto4);
        testEntityManager.persist(pto5);
        testEntityManager.persist(pto6);

        List<PtoEntity> result = ptoRepository.findAllOverlappingRequests(testAppUser, testingEnd, testingStart);
        assertEquals(List.of(pto1, pto2, pto5), result);
    }

    @Test
    void find_requests_by_supervisor_and_time_frame_should_return_requests_related_to_given_timeframe_and_supervisor() {
        AppUserEntity supervisor = AppUserEntity.createTestAppUser("supervisor", "supervisor", "supervisor@test.com");
        AppUserEntity testAppUser = AppUserEntity.createTestAppUser("test", "test", "test@test.com");
        testAppUser.setSupervisor(supervisor);
        AppUserEntity testAppUser2 = AppUserEntity.createTestAppUser("test2", "test2", "test2@test.com");

        PtoEntity pto1 = PtoEntity.builder()
                .applier(testAppUser)
                .ptoStart(LocalDate.of(2024, 12, 12))
                .ptoEnd(LocalDate.of(2025, 1, 12))
                .wasAccepted(true)
                .decisionDateTime(OffsetDateTime.now(ZoneOffset.UTC))
                .build();
        PtoEntity pto2 = PtoEntity.builder()
                .applier(testAppUser)
                .ptoStart(LocalDate.of(2024, 12, 12))
                .ptoEnd(LocalDate.of(2025, 2, 12))
                .wasAccepted(true)
                .decisionDateTime(OffsetDateTime.now(ZoneOffset.UTC))
                .build();

        PtoEntity falsePto1 = PtoEntity.builder()
                .applier(testAppUser)
                .ptoStart(LocalDate.of(2024, 12, 12))
                .ptoEnd(LocalDate.of(2025, 2, 12))
                .decisionDateTime(OffsetDateTime.now(ZoneOffset.UTC))
                .wasWithdrawn(true)
                .build();
        PtoEntity falsePto2 = PtoEntity.builder()
                .applier(testAppUser)
                .ptoStart(LocalDate.of(2025, 12, 12))
                .ptoEnd(LocalDate.of(2026, 2, 12))
                .decisionDateTime(OffsetDateTime.now(ZoneOffset.UTC))
                .wasWithdrawn(true)
                .build();
        PtoEntity falsePto3 = PtoEntity.builder()
                .applier(testAppUser2)
                .ptoStart(LocalDate.of(2024, 12, 12))
                .ptoEnd(LocalDate.of(2025, 1, 12))
                .wasAccepted(true)
                .decisionDateTime(OffsetDateTime.now(ZoneOffset.UTC))
                .build();

        LocalDate testingStart = LocalDate.of(2025, 1, 1);
        LocalDate testingEnd = LocalDate.of(2025, 1, 31);

        testEntityManager.persist(supervisor);
        testEntityManager.persist(testAppUser);
        testEntityManager.persist(testAppUser2);
        testEntityManager.persist(pto1);
        testEntityManager.persist(pto2);
        testEntityManager.persist(falsePto1);
        testEntityManager.persist(falsePto2);
        testEntityManager.persist(falsePto3);

        List<PtoEntity> result = ptoRepository.findRequestsBySupervisorAndTimeFrame(1L, testingStart, testingEnd);

        assertEquals(2, result.size());
        assertEquals(1L, result.get(0).getPtoRequestId());
        assertEquals(2L, result.get(1).getPtoRequestId());
    }
}