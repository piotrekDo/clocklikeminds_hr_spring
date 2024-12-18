package com.example.clocklike_portal.appUser;

import com.example.clocklike_portal.job_position.PositionEntity;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@AllArgsConstructor
public class EmployeeInfo {
    private final Long appUserId;
    private final boolean isFreelancer;
    private final boolean isActive;
    private final long supervisorId;
    private final String supervisorFirstName;
    private final String supervisorLastname;
    private final String firstName;
    private final String lastName;
    private final String userEmail;
    private final String imageUrl;
    private final PositionEntity position;
    private final LocalDate hireStart;
    private final LocalDate hireEnd;
    private final int ptoDaysAccruedLastYear;
    private final int ptoDaysAccruedCurrentYear;
    private final int ptoDaysLeftFromLastYear;
    private final int ptoDaysLeftCurrentYear;
    private final int ptoDaysTaken;
    private final boolean onTimeOff;
    private final boolean incomingTimeOff;


}
