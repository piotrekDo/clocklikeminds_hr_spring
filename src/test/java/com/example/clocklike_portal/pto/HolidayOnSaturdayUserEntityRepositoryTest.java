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
}