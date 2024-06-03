package com.example.clocklike_portal.pto;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface HolidayOnSaturdayRepository extends JpaRepository<HolidayOnSaturdayEntity, Long> {

    Optional<HolidayOnSaturdayEntity> findByDate(LocalDate date);

    @Query("SELECT h FROM HolidayOnSaturdayEntity h WHERE YEAR(h.date) = :year ORDER BY h.date DESC")
    List<HolidayOnSaturdayEntity> findAllByYearOrderByDateDesc(@Param("year") int year);

    @Query("SELECT h FROM HolidayOnSaturdayEntity h WHERE YEAR(h.date) = :year ORDER BY h.date DESC")
    HolidayOnSaturdayEntity findLastRegisteredHolidayOnSaturdayByYear(@Param("year") int year);

    HolidayOnSaturdayEntity findFirstByOrderByDateDesc();
}
