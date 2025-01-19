package com.example.clocklike_portal.dates_calculations;

import com.example.clocklike_portal.error.IllegalOperationException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
public class DateChecker {

    private DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private final LocalDate now = LocalDate.now();

    public void setDateFormatter(DateTimeFormatter dateFormatter) {
        this.dateFormatter = dateFormatter;
    }

    public boolean checkIfDatesRangeIsValid(String start, String to) {
        LocalDate startDate = LocalDate.parse(start, dateFormatter);
        LocalDate toDate = LocalDate.parse(to, dateFormatter);
        return checkIfDatesRangeIsValid(startDate, toDate);
    }

    public boolean checkIfDatesRangeIsValid(LocalDate start, LocalDate to) {
        if (start.getYear() < now.getYear()) {
            throw new IllegalOperationException("Time-off requests for previous years are not allowed.");
        }
        return to.isAfter(start) || to.isEqual(start);
    }
}
