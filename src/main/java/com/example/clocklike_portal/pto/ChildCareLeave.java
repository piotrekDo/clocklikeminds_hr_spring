package com.example.clocklike_portal.pto;

import com.example.clocklike_portal.appUser.AppUserEntity;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.*;

import java.time.LocalDate;

@EqualsAndHashCode(callSuper = true)
@Entity
@DiscriminatorValue("child_care_leave")
@NoArgsConstructor
@Data
@ToString
public class ChildCareLeave extends PtoEntity{
    public ChildCareLeave(LocalDate ptoStart, LocalDate ptoEnd, AppUserEntity applier, AppUserEntity acceptor, int businessDays, int includingLastYearPool) {
        super("childCare", false, ptoStart, ptoEnd, applier, acceptor, businessDays, includingLastYearPool);
    }


}
