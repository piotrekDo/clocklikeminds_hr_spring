package com.example.clocklike_portal.appUser;

import org.springframework.cglib.core.Local;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AppUserRepository extends JpaRepository<AppUserEntity, Long> {
    Optional<AppUserEntity> findByUserEmailIgnoreCase(String userEmail);

    List<AppUserEntity> findAllByUserRolesContaining(UserRole role);

    List<AppUserEntity> findAllByIsRegistrationFinishedFalseOrIsActiveFalse();

    @Query("SELECT a FROM AppUserEntity a WHERE a.isFreelancer = false")
    List<AppUserEntity> findAllByFreelancerIsFalse();

    Page<AppUserEntity> findAllBySupervisor_AppUserId(long supervisorId, PageRequest page);

    @Query("""
                SELECT new com.example.clocklike_portal.appUser.EmployeeInfo(
                    u.appUserId,
                    u.isFreelancer,
                    u.isActive,
                    u.supervisor.appUserId,
                    u.supervisor.firstName,
                    u.supervisor.lastName,
                    u.firstName,
                    u.lastName,
                    u.userEmail,
                    u.imageUrl,
                    u.position,
                    u.hireStart,
                    u.hireEnd,
                    u.ptoDaysAccruedLastYear,
                    u.ptoDaysAccruedCurrentYear,
                    u.ptoDaysLeftFromLastYear,
                    u.ptoDaysLeftCurrentYear,
                    u.ptoDaysTaken,
                    COALESCE(
                        (SELECT r.wasAccepted
                         FROM u.ptoRequests r
                         WHERE r.wasAccepted = TRUE
                         AND :today BETWEEN r.ptoStart AND r.ptoEnd
                         ORDER BY r.ptoRequestId desc 
                         LIMIT 1),
                        false
                    ),
                    COALESCE(
                        (SELECT r.wasAccepted
                         FROM u.ptoRequests r
                         WHERE r.wasAccepted = TRUE
                         AND (r.ptoStart > :today AND r.ptoStart <= :inWeek)
                         ORDER BY r.ptoRequestId desc 
                         LIMIT 1),
                        false
                    )
                )
                FROM AppUserEntity u
                LEFT JOIN u.supervisor s
                WHERE u.supervisor.appUserId = :supervisorId
            """)
    List<EmployeeInfo> findAllEmployeesBySupervisorId(@Param("supervisorId") long supervisorId, @Param("today")LocalDate today, @Param("inWeek") LocalDate inWeek);



    @Query("""
                SELECT new com.example.clocklike_portal.appUser.EmployeeInfo(
                    u.appUserId,
                    u.isFreelancer,
                    u.isActive,
                    u.supervisor.appUserId,
                    u.supervisor.firstName,
                    u.supervisor.lastName,
                    u.firstName,
                    u.lastName,
                    u.userEmail,
                    u.imageUrl,
                    u.position,
                    u.hireStart,
                    u.hireEnd,
                    u.ptoDaysAccruedLastYear,
                    u.ptoDaysAccruedCurrentYear,
                    u.ptoDaysLeftFromLastYear,
                    u.ptoDaysLeftCurrentYear,
                    u.ptoDaysTaken,
                    COALESCE(
                        (SELECT r.wasAccepted
                         FROM u.ptoRequests r
                         WHERE r.wasAccepted = TRUE
                         AND :today BETWEEN r.ptoStart AND r.ptoEnd
                         ORDER BY r.ptoRequestId desc 
                         LIMIT 1),
                        false
                    ),
                    COALESCE(
                        (SELECT r.wasAccepted
                         FROM u.ptoRequests r
                         WHERE r.wasAccepted = TRUE
                         AND (r.ptoStart > :today AND r.ptoStart <= :inWeek)
                         ORDER BY r.ptoRequestId desc 
                         LIMIT 1),
                        false
                    )
                )
                FROM AppUserEntity u
                    LEFT JOIN u.supervisor s
            """)
    List<EmployeeInfo> findAllEmployeesForAdmin(@Param("today")LocalDate today, @Param("inWeek") LocalDate inWeek);


}
