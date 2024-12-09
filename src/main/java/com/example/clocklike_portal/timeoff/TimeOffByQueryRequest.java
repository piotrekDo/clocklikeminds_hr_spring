package com.example.clocklike_portal.timeoff;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class TimeOffByQueryRequest {
    private Long id;
    private Long employeeId;
    private String employeeEmail;
    private Long acceptorId;
    private String acceptorEmail;
    private Boolean wasAccepted;
    private Boolean wasRejected;
    private Boolean isPending;
    private Boolean useOr;
    private String requestDateFrom;
    private String requestDateTo;
    private String ptoStartFrom;
    private String ptoStartTo;
    private String ptoEndFrom;
    private String ptoEndTo;
}
