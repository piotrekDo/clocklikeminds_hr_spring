package com.example.clocklike_portal.pto;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class SaturdayHolidayDto {
    private long id;
    private String date;
    private String note;
    private String usedDate;
}
