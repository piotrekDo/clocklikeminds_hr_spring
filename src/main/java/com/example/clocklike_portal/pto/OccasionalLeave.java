package com.example.clocklike_portal.pto;

import com.example.clocklike_portal.appUser.AppUserEntity;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import lombok.*;

import java.time.LocalDate;

@EqualsAndHashCode(callSuper = true)
@Entity
@DiscriminatorValue("occasional_leave")
@AllArgsConstructor
@NoArgsConstructor
@Data
@ToString
public class OccasionalLeave extends PtoEntity {
    private String leaveReason;
    @ManyToOne
    private OccasionalLeaveType occasionalType;

    public OccasionalLeave(LocalDate ptoStart, LocalDate ptoEnd, AppUserEntity applier, AppUserEntity acceptor, int businessDays, int includingLastYearPool, String leaveReason, OccasionalLeaveType occasionalType) {
        super("occasionalLeave", false, ptoStart, ptoEnd, applier, acceptor, businessDays, includingLastYearPool);
        this.leaveReason = leaveReason;
        this.occasionalType = occasionalType;
    }
}
