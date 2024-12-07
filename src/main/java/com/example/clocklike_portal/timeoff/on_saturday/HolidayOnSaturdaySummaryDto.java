package com.example.clocklike_portal.timeoff.on_saturday;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@AllArgsConstructor
@Data
public class HolidayOnSaturdaySummaryDto {
    private SaturdayHolidayDto nextHolidayOnSaturday;
    private int nextHolidayOnSaturdayInDays;
    private List<SaturdayHolidayDto> currentYearHolidaysOnSaturday;
}
