package com.example.clocklike_portal.timeoff;

import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;

@AllArgsConstructor
@Data
public class ResolvePtoRequest {
    @NonNull
    @Positive
    private Long ptoRequestId;
    @NonNull
    private Boolean isAccepted;
    private String declineReason;
}
