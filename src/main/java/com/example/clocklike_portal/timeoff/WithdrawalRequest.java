package com.example.clocklike_portal.timeoff;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class WithdrawalRequest {
    private Long requestId;
    private String ptoType;
    private String occasionalType;
    private String saturdayHolidayDate;
}
