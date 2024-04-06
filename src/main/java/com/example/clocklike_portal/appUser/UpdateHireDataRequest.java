package com.example.clocklike_portal.appUser;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UpdateHireDataRequest {
    private Long appUserId;
    private String positionKey;
    private String positionChangeDate;
    private String workStartDate;
    private String workEndDate;
}
