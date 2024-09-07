package com.example.clocklike_portal.timeoff;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;

@AllArgsConstructor
@Data
@ToString
public class WithdrawResponse {
    private long requestId;
    private long applierId;
    private boolean wasDeleted;
    private boolean setToDelete;
}
