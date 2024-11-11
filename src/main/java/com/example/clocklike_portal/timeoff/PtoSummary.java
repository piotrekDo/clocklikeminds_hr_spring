package com.example.clocklike_portal.timeoff;

import com.example.clocklike_portal.timeoff.on_saturday.SaturdayHolidayDto;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@AllArgsConstructor
@Data
public class PtoSummary {
    private int ptoDaysAccruedLastYear;
    private int ptoDaysAccruedCurrentYear;
    private int ptoDaysLeftFromLastYear;
    private int ptoDaysLeftCurrentYear;
    private int ptoDaysTaken;
    private List<SaturdayHolidayDto> saturdayHolidaysCurrentYear;
}
