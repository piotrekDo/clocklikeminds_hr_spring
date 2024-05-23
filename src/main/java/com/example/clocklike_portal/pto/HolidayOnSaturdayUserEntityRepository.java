package com.example.clocklike_portal.pto;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface HolidayOnSaturdayUserEntityRepository extends JpaRepository<HolidayOnSaturdayUserEntity, Long> {

    Optional<HolidayOnSaturdayUserEntity> findByHolidayAndUser_AppUserId(HolidayOnSaturdayEntity holiday, Long userId);
}
