package com.example.clocklike_portal.pto;


import com.example.clocklike_portal.app.Library;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import lombok.*;

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
}
