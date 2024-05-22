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
@DiscriminatorValue(ChildCareLeaveEntity.DISCRIMINATOR_VALUE)
@NoArgsConstructor
@Data
@ToString
public class ChildCareLeaveEntity extends PtoEntity {
    static final String DISCRIMINATOR_VALUE = Library.CHILD_CARE_LEAVE_DISCRIMINATOR_VALUE;
    @ManyToOne
    private OccasionalLeaveType occasionalType;

    public ChildCareLeaveEntity(LocalDate ptoStart, LocalDate ptoEnd, AppUserEntity applier, AppUserEntity acceptor, int businessDays, OccasionalLeaveType occasionalType) {
        super(DISCRIMINATOR_VALUE, false, ptoStart, ptoEnd, applier, acceptor, businessDays, 0);
        this.occasionalType = occasionalType;
    }


}
