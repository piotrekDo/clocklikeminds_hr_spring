package com.example.clocklike_portal.dashboard;

import com.example.clocklike_portal.timeoff.TimeOffDto;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 *  return -1 for newEmployees and inactiveEmployees if supervisor is not admin.
 */

@AllArgsConstructor
@Data
public class SupervisorDashboardDto {
    private List<TimeOffDto> requestsForDashboardCalendar;
    private int unresolvedRequestsCount;
    private int requestToWithdraw;
    private int newEmployees;
    private int inactiveEmployees;
}
