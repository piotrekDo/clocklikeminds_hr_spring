package com.example.clocklike_portal.timeoff.on_saturday;

import com.example.clocklike_portal.timeoff.on_saturday.HolidayOnSaturdayPtoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HolidayOnSaturdayPtoEntityRepository extends JpaRepository<HolidayOnSaturdayPtoEntity, Long> {
}
