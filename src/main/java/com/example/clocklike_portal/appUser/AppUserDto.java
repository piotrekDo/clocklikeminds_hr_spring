package com.example.clocklike_portal.appUser;

import com.example.clocklike_portal.job_position.PositionEntity;
import com.example.clocklike_portal.job_position.PositionHistory;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@AllArgsConstructor
@Data
@ToString
public class AppUserDto {
    private Long appUserId;
    private String firstName;
    private String lastName;
    private String userEmail;
    private List<UserRole> userRoles;
    private boolean isActive;
    private boolean isStillHired;
    private PositionEntity position;
    private List<PositionHistory> positionHistory;
    private LocalDate hireStart;
    private LocalDate hireEnd;
    private long seniorityInMonths;
    private int ptoDaysFromLastYear;
    private int ptoDaysTotal;
    private int ptoDaysTaken;

    public static AppUserDto appUserEntityToDto(AppUserEntity entity) {
        long seniority = entity.getHireStart() != null ? ChronoUnit.MONTHS.between(entity.getHireStart(), LocalDate.now()) : 0;

        return new AppUserDto(
                entity.getAppUserId(),
                entity.getFirstName(),
                entity.getLastName(),
                entity.getUserEmail(),
                entity.getUserRoles().stream().toList(),
                entity.isActive(),
                entity.isStillHired(),
                entity.getPosition(),
                entity.getPositionHistory().stream().toList(),
                entity.getHireStart(),
                entity.getHireEnd(),
                seniority,
                entity.getPtoDaysFromLastYear(),
                entity.getPtoDaysTotal(),
                entity.getPtoDaysTaken()
        );
    }
}
