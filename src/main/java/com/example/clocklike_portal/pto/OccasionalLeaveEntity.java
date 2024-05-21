package com.example.clocklike_portal.pto;

import com.example.clocklike_portal.app.Library;
import com.example.clocklike_portal.appUser.AppUserEntity;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import lombok.*;

import java.time.LocalDate;

@EqualsAndHashCode(callSuper = true)
@Entity
@DiscriminatorValue(OccasionalLeaveEntity.DISCRIMINATOR_VALUE)
@AllArgsConstructor
@NoArgsConstructor
@Data
@ToString
public class OccasionalLeaveEntity extends PtoEntity {
    static final String DISCRIMINATOR_VALUE = Library.OCCASIONAL_LEAVE_DISCRIMINATOR_VALUE;
    private String leaveReason;
    @ManyToOne
    private OccasionalLeaveType occasionalType;

    public OccasionalLeaveEntity(LocalDate ptoStart, LocalDate ptoEnd, AppUserEntity applier, AppUserEntity acceptor, int businessDays, int includingLastYearPool, String leaveReason, OccasionalLeaveType occasionalType) {
        super("occasionalLeave", false, ptoStart, ptoEnd, applier, acceptor, businessDays, includingLastYearPool);
        this.leaveReason = leaveReason;
        this.occasionalType = occasionalType;
    }
}
