package com.example.clocklike_portal.appUser;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class UpdateEmployeeHolidayDataRequest {
    private Long appUserId;
    private Integer ptoTotalDaysNewValue;
    private Integer ptoDaysAcquiredLastYearNewValue;
}
