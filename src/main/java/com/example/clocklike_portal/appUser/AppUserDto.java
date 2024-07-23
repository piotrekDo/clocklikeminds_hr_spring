package com.example.clocklike_portal.appUser;

import com.example.clocklike_portal.job_position.PositionEntity;
import com.example.clocklike_portal.job_position.PositionHistory;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

@AllArgsConstructor
@Data
@ToString
public class AppUserDto {
    private Long appUserId;
    private boolean isFreelancer;
    private String firstName;
    private String lastName;
    private String userEmail;
    private String imageUrl;
    private List<UserRole> userRoles;
    private boolean isRegistrationFinished;
    private boolean isActive;
    private boolean isStillHired;
    private PositionEntity position;
    private List<PositionHistory> positionHistory;
    private LocalDate hireStart;
    private LocalDate hireEnd;
    private long seniorityInMonths;
    private int ptoDaysAccruedLastYear;
    private int ptoDaysAccruedCurrentYear;
    private int ptoDaysLeftFromLastYear;
    private int ptoDaysLeftTotal;
    private int ptoDaysTaken;
    private long supervisorId;
    private String supervisorFirstName;
    private String supervisorLastName;
    private List<AppUserBasicDto> subordinates;

    public static AppUserDto appUserEntityToDto(AppUserEntity entity) {
        long seniority = entity.getHireStart() != null ? ChronoUnit.MONTHS.between(entity.getHireStart(), LocalDate.now()) : 0;

        AppUserEntity supervisor = entity.getSupervisor();
        long supervisorId = 0;
        String supervisorFirstName = "";
        String supervisorLastName = "";
        if (supervisor != null) {
            supervisorId = supervisor.getAppUserId();
            supervisorFirstName = supervisor.getFirstName();
            supervisorLastName = supervisor.getLastName();
        }

        Set<AppUserEntity> subordinatesEntities = entity.getSubordinates();
        ArrayList<AppUserBasicDto> subordinates = new ArrayList<>();
        subordinatesEntities.forEach(subordinateEntity -> {
            subordinates.add(AppUserBasicDto.appUserEntityToBasicDto(subordinateEntity));
        });



        return new AppUserDto(
                entity.getAppUserId(),
                entity.isFreelancer(),
                entity.getFirstName(),
                entity.getLastName(),
                entity.getUserEmail(),
                entity.getImageUrl(),
                entity.getUserRoles().stream().toList(),
                entity.isRegistrationFinished(),
                entity.isActive(),
                entity.isStillHired(),
                entity.getPosition(),
                entity.getPositionHistory().stream().sorted(Comparator.comparing(PositionHistory::getStartDate).reversed()).toList(),
                entity.getHireStart(),
                entity.getHireEnd(),
                seniority,
                entity.getPtoDaysAccruedLastYear(),
                entity.getPtoDaysAccruedCurrentYear(),
                entity.getPtoDaysLeftFromLastYear(),
                entity.getPtoDaysLeftCurrentYear(),
                entity.getPtoDaysTaken(),
                supervisorId,
                supervisorFirstName,
                supervisorLastName,
                subordinates
        );
    }
}
