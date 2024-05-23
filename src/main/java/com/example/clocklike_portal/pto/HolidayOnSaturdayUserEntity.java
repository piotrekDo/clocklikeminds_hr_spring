package com.example.clocklike_portal.pto;

import com.example.clocklike_portal.appUser.AppUserEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class HolidayOnSaturdayUserEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne
    private HolidayOnSaturdayEntity holiday;
    @ManyToOne
    private AppUserEntity user;
    @OneToOne
    private PtoEntity pto;

    public HolidayOnSaturdayUserEntity(HolidayOnSaturdayEntity holiday, AppUserEntity user) {
        this.holiday = holiday;
        this.user = user;
    }
}
