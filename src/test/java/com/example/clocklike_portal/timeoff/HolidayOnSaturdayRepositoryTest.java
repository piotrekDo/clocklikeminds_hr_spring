package com.example.clocklike_portal.timeoff;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
@ExtendWith(SpringExtension.class)
@DataJpaTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class HolidayOnSaturdayRepositoryTest {

    @Autowired
    TestEntityManager testEntityManager;

    @Autowired
    HolidayOnSaturdayRepository repository;

    @Test
    void findAllByDateGreaterThanEqualShouldReturnCorrespondingRecords() {
        HolidayOnSaturdayEntity matching1 = testEntityManager.persist(new HolidayOnSaturdayEntity(LocalDate.of(2024, 5, 10), "matching1"));
        HolidayOnSaturdayEntity matching2 = testEntityManager.persist(new HolidayOnSaturdayEntity(LocalDate.of(2024, 8, 23), "matching2"));
        HolidayOnSaturdayEntity unMatching = testEntityManager.persist(new HolidayOnSaturdayEntity(LocalDate.of(2024, 5, 9), "unMatching"));

        List<HolidayOnSaturdayEntity> result = repository.findAllByDateGreaterThanEqual(LocalDate.of(2024, 5, 10));

        assertEquals(2, result.size());
        assertEquals("matching1", result.get(0).getNote());
        assertEquals("matching2", result.get(1).getNote());
    }
}
