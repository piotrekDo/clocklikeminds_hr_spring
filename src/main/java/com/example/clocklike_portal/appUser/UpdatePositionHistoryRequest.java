package com.example.clocklike_portal.appUser;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;

@AllArgsConstructor
@Data
public class UpdatePositionHistoryRequest {
    private Long positionHistoryId;
    private String startDate;
}
