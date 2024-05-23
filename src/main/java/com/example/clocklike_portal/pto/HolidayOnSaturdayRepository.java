package com.example.clocklike_portal.pto;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface HolidayOnSaturdayRepository extends JpaRepository<HolidayOnSaturdayEntity, Long> {

    Optional<HolidayOnSaturdayEntity> findByDate(LocalDate date);
}
