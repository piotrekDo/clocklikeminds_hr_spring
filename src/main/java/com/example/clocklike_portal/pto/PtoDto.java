package com.example.clocklike_portal.pto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@AllArgsConstructor
@Data
public class PtoDto {
    private Long id;
    private boolean isPending;
    private boolean wasAccepted;
    private LocalDateTime requestDateTime;
    private LocalDate ptoStart;
    private LocalDate ptoEnd;
    private long applierId;
    private String applierFirstName;
    private String applierLastName;
    private String applierEmail;
    private int applierPtoDaysTotal;
    private int applierPtoDaysTaken;
    private String applierImageUrl;
    private long acceptorId;
    private String acceptorFirstName;
    private String acceptorLastName;
    private String acceptorEmail;
    private LocalDateTime decisionDateTime;
    private long totalDays;
    private int businessDays;
    private int includingLastYearPool;
    private String declineReason;
}
