package com.example.clocklike_portal.timeoff.on_saturday;


import com.example.clocklike_portal.app.Library;
import com.example.clocklike_portal.appUser.AppUserEntity;
import com.example.clocklike_portal.timeoff.PtoEntity;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import lombok.*;

import java.time.LocalDate;

@EqualsAndHashCode(callSuper = true)
@Entity
@DiscriminatorValue(HolidayOnSaturdayPtoEntity.DISCRIMINATOR_VALUE)
@AllArgsConstructor
@NoArgsConstructor
@Data
@ToString
public class HolidayOnSaturdayPtoEntity extends PtoEntity {
    static final String DISCRIMINATOR_VALUE = Library.ON_SATURDAY_PTO_DISCRIMINATOR_VALUE;
    @ManyToOne
    private HolidayOnSaturdayEntity holiday;

    public HolidayOnSaturdayPtoEntity(LocalDate ptoStart, AppUserEntity applier, AppUserEntity acceptor, HolidayOnSaturdayEntity holiday) {
        super(DISCRIMINATOR_VALUE, false, ptoStart, ptoStart, applier, acceptor, 1, 0);
        this.holiday = holiday;
    }
}
