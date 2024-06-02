package com.example.clocklike_portal.pto;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Data
public class HolidayOnSaturdayEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(unique = true)
    private LocalDate date;
    private String note;

    public HolidayOnSaturdayEntity(LocalDate date, String note) {
        this.date = date;
        this.note = note;
    }
}
