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
    private boolean isFreelancer;
    private String firstName;
    private String lastName;
    private String userEmail;
    private String imageUrl;
    private boolean isRegistrationFinished;
    private boolean isActive;
    private boolean isStillHired;
    private PositionEntity position;
    private long seniorityInMonths;
    private int status;
    private int ptoDaysAccruedLastYear;
    private int ptoDaysAccruedCurrentYear;
    private int ptoDaysLeftFromLastYear;
    private int ptoDaysLeftTotal;
    private int ptoDaysTaken;

    public static AppUserBasicDto appUserEntityToBasicDto(AppUserEntity entity) {
        long seniority = entity.getHireStart() != null ? ChronoUnit.MONTHS.between(entity.getHireStart(), LocalDate.now()) : 0;
        int status = entity.isActive() ? 1 : !entity.isRegistrationFinished() ? 2 : !entity.isActive() ? 3 : 0;

        return new AppUserBasicDto(
                entity.getAppUserId(),
                entity.isFreelancer(),
                entity.getFirstName(),
                entity.getLastName(),
                entity.getUserEmail(),
                entity.getImageUrl(),
                entity.isRegistrationFinished(),
                entity.isActive(),
                entity.isStillHired(),
                entity.getPosition(),
                seniority,
                status,
                entity.getPtoDaysAccruedLastYear(),
                entity.getPtoDaysAccruedCurrentYear(),
                entity.getPtoDaysLeftFromLastYear(),
                entity.getPtoDaysLeftCurrentYear(),
                entity.getPtoDaysTaken()
        );
    }
}
