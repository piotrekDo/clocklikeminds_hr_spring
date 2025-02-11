package com.example.clocklike_portal.timeoff_history;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@AllArgsConstructor
public class RequestHistoryDto {
    private long historyId;
    private String action;
    private String notes;
    private OffsetDateTime dateTime;
    private Long appUserId;
    private String firstName;
    private String lastName;
    private String userEmail;
    private String imageUrl;
    private long timeOffRequestId;
}
