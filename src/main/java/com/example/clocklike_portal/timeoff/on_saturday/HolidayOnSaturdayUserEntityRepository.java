package com.example.clocklike_portal.timeoff.on_saturday;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HolidayOnSaturdayUserEntityRepository extends JpaRepository<HolidayOnSaturdayUserEntity, Long> {

    Optional<HolidayOnSaturdayUserEntity> findByHolidayAndUser_AppUserId(HolidayOnSaturdayEntity holiday, Long userId);

    @Query("SELECT hsu FROM HolidayOnSaturdayUserEntity hsu " +
            "JOIN hsu.holiday h " +
            "LEFT JOIN FETCH hsu.pto p " +
            "WHERE YEAR(h.date) = :year")
    List<HolidayOnSaturdayUserEntity> findAllByHolidayYear(@Param("year") int year);

    @Query("SELECT hsu FROM HolidayOnSaturdayUserEntity hsu " +
            "WHERE hsu.user.appUserId = :userId AND YEAR(hsu.holiday.date) = :year")
    List<HolidayOnSaturdayUserEntity> findAllByUserIdAndYear(@Param("userId") Long userId, @Param("year") int year);

    @Query("SELECT hsu FROM HolidayOnSaturdayUserEntity hsu " +
            "WHERE hsu.user.appUserId = :userId AND YEAR(hsu.holiday.date) = :year " +
            "AND MONTH(hsu.holiday.date) <= :currentMonth")
    List<HolidayOnSaturdayUserEntity> findAllByUserIdAndYearUpToCurrentMonth(
            @Param("userId") Long userId,
            @Param("year") int year,
            @Param("currentMonth") int currentMonth);
}
