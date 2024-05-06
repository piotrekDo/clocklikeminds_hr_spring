package com.example.clocklike_portal.pto;

import com.example.clocklike_portal.appUser.AppUserEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@DataJpaTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class PtoRepositoryTest {
    @Autowired
    PtoRepository ptoRepository;

    @Autowired
    TestEntityManager testEntityManager;

    @Test
    void find_requests_by_acceptor_and_time_frame_should_return_corresponding_requests() {
        AppUserEntity acceptor = testEntityManager.persist(AppUserEntity.createTestAppUser("acceptor", "acceptor", "acceptor@mail.com"));
        AppUserEntity acceptor2 = testEntityManager.persist(AppUserEntity.createTestAppUser("acceptor", "acceptor", "acceptor2@mail.com"));
        PtoEntity unMatching1 = testEntityManager.persist(PtoEntity.builder().acceptor(acceptor2).ptoStart(LocalDate.of(2024, 5, 10)).ptoEnd(LocalDate.of(2024, 5, 12)).build());
        PtoEntity unMatching2 = testEntityManager.persist(PtoEntity.builder().acceptor(acceptor2).ptoStart(LocalDate.of(2024, 5, 10)).ptoEnd(LocalDate.of(2024, 5, 12)).decisionDateTime(LocalDateTime.now()).wasAccepted(true).build());
        PtoEntity unMatching3 = testEntityManager.persist(PtoEntity.builder().acceptor(acceptor).ptoStart(LocalDate.of(2024, 4, 30)).ptoEnd(LocalDate.of(2024, 5, 12)).build());
        PtoEntity unMatching4 = testEntityManager.persist(PtoEntity.builder().acceptor(acceptor).ptoStart(LocalDate.of(2024, 6, 1)).ptoEnd(LocalDate.of(2024, 5, 12)).build());
        PtoEntity matching1 = testEntityManager.persist(PtoEntity.builder().acceptor(acceptor).ptoStart(LocalDate.of(2024, 5, 1)).ptoEnd(LocalDate.of(2024, 5, 12)).build());
        PtoEntity matching2 = testEntityManager.persist(PtoEntity.builder().acceptor(acceptor).ptoStart(LocalDate.of(2024, 5, 12)).ptoEnd(LocalDate.of(2024, 7, 1)).build());
        PtoEntity matching3 = testEntityManager.persist(PtoEntity.builder().acceptor(acceptor).ptoStart(LocalDate.of(2024, 5, 31)).ptoEnd(LocalDate.of(2024, 7, 1)).build());

        LocalDate start = LocalDate.of(2024, 5, 1);
        LocalDate end = LocalDate.of(2024, 5, 31);

        List<PtoEntity> result = ptoRepository.findRequestsByAcceptorAndTimeFrame(1L, start, end);

        result.forEach(x -> System.out.println(x.getPtoStart()
        ));
        assertEquals(3, result.size());
        assertEquals(matching1.getPtoRequestId(), result.get(0).getPtoRequestId());
        assertEquals(matching2.getPtoRequestId(), result.get(1).getPtoRequestId());
        assertEquals(matching3.getPtoRequestId(), result.get(2).getPtoRequestId());
    }

    @Test
    void find_requests_for_year_should_return_requests_for_given_year_and_january_next_year_and_december_last_year() {
        //given
        int givenYear = 2024;
        PtoEntity givenYearMidYear = new PtoEntity(LocalDate.of(2024, 5, 10), LocalDate.of(2024, 5, 12), null, null, 0, 0);
        PtoEntity givenYearDec = new PtoEntity(LocalDate.of(2024, 12, 10), LocalDate.of(2024, 12, 12), null, null, 0, 0);
        PtoEntity givenYearDecToJan = new PtoEntity(LocalDate.of(2024, 12, 26), LocalDate.of(2025, 1, 3), null, null, 0, 0);
        PtoEntity givenYearJanDecPrev = new PtoEntity(LocalDate.of(2023, 12, 26), LocalDate.of(2024, 1, 3), null, null, 0, 0);
        PtoEntity prevYearDec = new PtoEntity(LocalDate.of(2023, 11, 26), LocalDate.of(2023, 12, 7), null, null, 0, 0);
        PtoEntity nextYearJan = new PtoEntity(LocalDate.of(2025, 1, 26), LocalDate.of(2025, 2, 7), null, null, 0, 0);
        PtoEntity nextYearMidYear = new PtoEntity(LocalDate.of(2025, 7, 12), LocalDate.of(2025, 7, 17), null, null, 0, 0);
        PtoEntity prevYearMidYear = new PtoEntity(LocalDate.of(2025, 6, 12), LocalDate.of(2025, 7, 17), null, null, 0, 0);
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
                .decisionDateTime(LocalDateTime.now())
                .build();
        PtoEntity pto2 = PtoEntity.builder()
                .applier(testAppUser)
                .ptoStart(LocalDate.of(2024, 1, 5))
                .ptoEnd(LocalDate.of(2024, 1, 7))
                .wasAccepted(true)
                .decisionDateTime(LocalDateTime.now())
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
                .decisionDateTime(LocalDateTime.now())
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
}