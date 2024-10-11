package com.example.clocklike_portal.timeoff;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@AllArgsConstructor
@Data
public class TimeOffDto {

    // TODO usunieto notki poza application, usunieto decline reason dodano historie
    private Long id;
    private String leaveType;
    private boolean isDemand;
    private String applicationNotes;
    private List<RequestHistoryDto> requestHistory;
    private boolean isPending;
    private boolean wasAccepted;
    private LocalDateTime requestDateTime;
    private LocalDate ptoStart;
    private LocalDate ptoEnd;
    private long applierId;
    private String applierFirstName;
    private String applierLastName;
    private String applierEmail;
    private boolean isApplierFreelancer;
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
    private String occasional_leaveReason;
    private Integer occasional_leaveTypeId;
    private String occasional_leaveType;
    private String occasional_descriptionPolish;
    private Integer occasional_days;
    private String saturday_holiday_date;
    private String saturday_holiday_desc;
    private boolean wasMarkedToWithdraw;
    private boolean wasWithdrawn;
    private LocalDateTime withdrawnDateTime;
    private String declineReason;
}