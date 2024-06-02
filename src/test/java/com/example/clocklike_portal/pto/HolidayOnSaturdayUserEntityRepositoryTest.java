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
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@DataJpaTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class HolidayOnSaturdayUserEntityRepositoryTest {
    @Autowired
    TestEntityManager testEntityManager;
    @Autowired
    HolidayOnSaturdayUserEntityRepository repository;

    @Test
    void findByHolidayAndUser_AppUserIdShouldReturnCorrespondingValue() {
        HolidayOnSaturdayEntity holiday = testEntityManager.persist(new HolidayOnSaturdayEntity(LocalDate.of(2024, 5, 25), "test note"));
        AppUserEntity appUser = testEntityManager.persist(AppUserEntity.createTestAppUser("user", "user", "user@user.com"));
        HolidayOnSaturdayUserEntity entity = testEntityManager.persist(new HolidayOnSaturdayUserEntity(holiday, appUser));

        Optional<HolidayOnSaturdayUserEntity> result = repository.findByHolidayAndUser_AppUserId(holiday, appUser.getAppUserId());

        assertTrue(result.isPresent());
    }

    @Test
    void findAllByHolidayYearShouldReturnRecordsOnlyForSelectedYear() {
        HolidayOnSaturdayEntity holiday = testEntityManager.persist(new HolidayOnSaturdayEntity(LocalDate.of(2024, 5, 25), "test note"));
        HolidayOnSaturdayEntity lastYearHoliday = testEntityManager.persist(new HolidayOnSaturdayEntity(LocalDate.of(2023, 5, 25), "test note"));
        HolidayOnSaturdayEntity nextYearHoliday = testEntityManager.persist(new HolidayOnSaturdayEntity(LocalDate.of(2025, 5, 25), "test note"));
        AppUserEntity appUser = testEntityManager.persist(AppUserEntity.createTestAppUser("user", "user", "user@user.com"));
        PtoEntity pto = testEntityManager.persist(new PtoEntity(false, null, null, appUser, null, 0, 0));
        HolidayOnSaturdayUserEntity matchingHoliday = new HolidayOnSaturdayUserEntity(holiday, appUser);
        matchingHoliday.setPto(pto);
        HolidayOnSaturdayUserEntity matching = testEntityManager.persist(matchingHoliday);
        HolidayOnSaturdayUserEntity unMatching = testEntityManager.persist(new HolidayOnSaturdayUserEntity(lastYearHoliday, appUser));
        HolidayOnSaturdayUserEntity unMatching2 = testEntityManager.persist(new HolidayOnSaturdayUserEntity(nextYearHoliday, appUser));

        List<HolidayOnSaturdayUserEntity> result = repository.findAllByHolidayYear(2024);

        assertEquals(1, result.size());
        assertEquals(matching.getId(), result.get(0).getId());
        assertNotNull(result.get(0).getPto());
    }
}