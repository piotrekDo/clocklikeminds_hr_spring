package com.example.clocklike_portal.timeoff;

import com.example.clocklike_portal.app.Library;
import com.example.clocklike_portal.appUser.AppUserEntity;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
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

    public ChildCareLeaveEntity(LocalDate ptoStart, LocalDate ptoEnd, AppUserEntity applier, AppUserEntity acceptor, int businessDays) {
        super(DISCRIMINATOR_VALUE, false, ptoStart, ptoEnd, applier, acceptor, businessDays, 0);
    }


}
