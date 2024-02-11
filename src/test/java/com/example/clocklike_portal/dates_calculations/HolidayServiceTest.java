package com.example.clocklike_portal.dates_calculations;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(SpringExtension.class)
class HolidayServiceTest {

    @Autowired
    HolidayService holidayService;

    @TestConfiguration
    static class HolidayServiceTestConfiguration {
        @Bean
        HolidayService holidayService() {
            return new HolidayService();
        }
    }

    @ParameterizedTest
    @ArgumentsSource(CalculatingEasterSundayArgumentsProvider.class)
    void calculating_easter_sunday_should_return_valid_value(int year, LocalDate expectedResult) {
        assertEquals(expectedResult, holidayService.calculateEasterSunday(year));
    }

    @ParameterizedTest
    @ArgumentsSource(CheckHolidayArgumentsProvider.class)
    void checking_holidays_should_return_valid_boolean(LocalDate checkedDate, boolean expectedResult) {
        assertEquals(expectedResult, holidayService.checkIfHoliday(checkedDate));
    }

}