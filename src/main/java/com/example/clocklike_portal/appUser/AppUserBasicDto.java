package com.example.clocklike_portal.appUser;

import com.example.clocklike_portal.job_position.PositionEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@AllArgsConstructor
@Data
@ToString
public class AppUserBasicDto {
    private Long appUserId;
    private String firstName;
    private String lastName;
    private String userEmail;
    private boolean isActive;
    private boolean isStillHired;
    private PositionEntity position;
    private long seniorityInMonths;

    public static AppUserBasicDto appUserEntityToBasicDto(AppUserEntity entity) {
        long seniority = entity.getHireStart() != null ? ChronoUnit.MONTHS.between(entity.getHireStart(), LocalDate.now()) : 0;

        return new AppUserBasicDto(
                entity.getAppUserId(),
                entity.getFirstName(),
                entity.getLastName(),
                entity.getUserEmail(),
                entity.isActive(),
                entity.isStillHired(),
                entity.getPosition(),
                seniority
        );
    }
}
