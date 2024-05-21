package com.example.clocklike_portal.pto;

import com.example.clocklike_portal.appUser.AppUserEntity;
import jakarta.persistence.*;

import java.util.List;

@Entity
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
}
