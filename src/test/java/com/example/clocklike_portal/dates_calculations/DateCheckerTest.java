package com.example.clocklike_portal.dates_calculations;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
class DateCheckerTest {

    @Autowired
    DateChecker dateChecker;

    @TestConfiguration
    static class DateCheckerTestConfiguration {
        @Bean
        DateChecker dateChecker() {
            return new DateChecker();
        }
    }

    @Test
    void checking_two_dame_dates_should_return_true() {
        LocalDate start = LocalDate.of(2023, 5, 1);
        LocalDate end = LocalDate.of(2023, 5, 1);

        assertTrue(dateChecker.checkIfDatesRangeIsValid(start, end));
    }

    @Test
    void checking_dates_from_two_strings_should_return_true_if_second_date_is_after() {
        String start = "2024-01-01";
        String end = "2024-01-02";

        assertTrue(dateChecker.checkIfDatesRangeIsValid(start, end));
    }

    @Test
    void checking_dates_from_two_strings_should_return_true_if_second_date_is_equal() {
        String start = "2023-05-01";
        String end = "2023-05-01";

        assertTrue(dateChecker.checkIfDatesRangeIsValid(start, end));
    }

    @Test
    void checking_dates_from_two_strings_should_return_true_if_second_date_is_before() {
        String start = "2024-05-01";
        String end = "2024-01-02";

        assertFalse(dateChecker.checkIfDatesRangeIsValid(start, end));
    }
}