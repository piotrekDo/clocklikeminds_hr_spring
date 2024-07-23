package com.example.clocklike_portal.timeoff;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HolidayOnSaturdayPtoEntityRepository extends JpaRepository<HolidayOnSaturdayPtoEntity, Long> {
}
