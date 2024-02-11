package com.example.clocklike_portal.dates_calculations;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
public class DateChecker {

    private DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public void setDateFormatter(DateTimeFormatter dateFormatter) {
        this.dateFormatter = dateFormatter;
    }

    public boolean checkIfDatesRangeIsValid(String start, String to) {
        LocalDate startDate = LocalDate.parse(start, dateFormatter);
        LocalDate toDate = LocalDate.parse(to, dateFormatter);
        return checkIfDatesRangeIsValid(startDate, toDate);
    }

    public boolean checkIfDatesRangeIsValid(LocalDate start, LocalDate to) {
        return to.isAfter(start) || to.isEqual(start);
    }
}
