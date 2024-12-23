package com.example.clocklike_portal.timeoff;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Data
public class RequestsForUserCalendar {
    private List<MonthSummary> months = new ArrayList<>();

    @AllArgsConstructor
    @Getter
    static class MonthSummary {
        private int monthIndexJava;
        private int monthIndexJs;
        private String monthName;
        private int workingHours;
        private int hoursWorked;
        private List<TimeOffDto> requests;
    }
}
