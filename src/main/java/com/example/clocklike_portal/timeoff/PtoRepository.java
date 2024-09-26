package com.example.clocklike_portal.timeoff;

import com.example.clocklike_portal.appUser.AppUserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface PtoRepository extends JpaRepository<PtoEntity, Long> {

    List<PtoEntity> findAllByDecisionDateTimeIsNull();

    List<PtoEntity> findAllByAcceptor_appUserId(long id);

    @Query("SELECT p FROM pto_requests p WHERE p.acceptor.appUserId = :acceptor " +
            "AND (p.decisionDateTime IS NULL OR (p.wasMarkedToWithdraw = true AND p.wasWithdrawn = false))")
    List<PtoEntity> findUnresolvedOrWithdrawnRequestsByAcceptorId(@Param("acceptor") long id);

    List<PtoEntity> findAllByDecisionDateTimeIsNullAndAcceptor_AppUserId(long id);

    List<PtoEntity> findAllByDecisionDateTimeIsNullAndApplier_appUserId(long id);

    Page<PtoEntity> findAllByApplier_AppUserId(long id, PageRequest pageable);

    @Query("SELECT p FROM pto_requests p WHERE (p.wasAccepted = true OR p.decisionDateTime IS NULL) AND (YEAR(p.ptoStart) = :year OR YEAR(p.ptoEnd) = :year) " +
            "OR (MONTH(p.ptoStart) = 12 AND YEAR(p.ptoStart) = :year -1) " +
            "OR (MONTH(p.ptoEnd) = 12 AND YEAR(p.ptoEnd) = :year -1)" +
            "OR (MONTH(p.ptoEnd) = 1 AND YEAR(p.ptoEnd) = :year + 1)" +
            "OR (MONTH(p.ptoStart) = 1 AND YEAR(p.ptoStart) = :year + 1)")
    List<PtoEntity> findRequestsForYear(@Param("year") int year);

    @Query("SELECT p FROM pto_requests p WHERE (p.wasAccepted = true OR p.decisionDateTime IS NULL) " +
            "AND (p.applier.appUserId = :userId) " +
            "AND (YEAR(p.ptoStart) = :year OR YEAR(p.ptoEnd) = :year) " +
            "OR (MONTH(p.ptoStart) = 12 AND YEAR(p.ptoStart) = :year -1) " +
            "OR (MONTH(p.ptoEnd) = 12 AND YEAR(p.ptoEnd) = :year -1)" +
            "OR (MONTH(p.ptoEnd) = 1 AND YEAR(p.ptoEnd) = :year + 1)" +
            "OR (MONTH(p.ptoStart) = 1 AND YEAR(p.ptoStart) = :year + 1)")
    List<PtoEntity> findRequestsForYear(@Param("year") int year, @Param("userId") Long userId);

    @Query("SELECT p FROM pto_requests p WHERE (p.acceptor.appUserId = :acceptorID) AND " +
            "(p.wasAccepted = TRUE OR p.decisionDateTime IS NULL) AND " +
            "((p.ptoStart >= :start AND p.ptoStart <= :end) OR " +
            "(p.ptoEnd >= :start AND p.ptoEnd <= :end) OR " +
            "(p.ptoStart < :start AND p.ptoEnd > :end)) " +
            "ORDER BY p.requestDateTime DESC")
    List<PtoEntity> findRequestsByAcceptorAndTimeFrame(@Param("acceptorID") Long acceptorID,
                                                       @Param("start") LocalDate start,
                                                       @Param("end") LocalDate end);

    @Query("SELECT p FROM pto_requests p " +
            "WHERE p.applier = :applier " +
            "AND (p.decisionDateTime IS NULL OR p.wasAccepted = true) " +
            "AND p.ptoStart <= :ptoEnd " +
            "AND p.ptoEnd >= :ptoStart")
    List<PtoEntity> findAllOverlappingRequests(
            @Param("applier") AppUserEntity applier,
            @Param("ptoEnd") LocalDate ptoEnd,
            @Param("ptoStart") LocalDate ptoStart
    );

    @Query("SELECT p FROM pto_requests p WHERE p.applier.appUserId = :appUserId AND p.isDemand = true AND FUNCTION('YEAR', p.requestDateTime) = FUNCTION('YEAR', CURRENT_DATE)")
    List<PtoEntity> findUserRequestsOnDemandFromCurrentYear(@Param("appUserId") Long appUserId);

    @Query("SELECT p FROM pto_requests p WHERE p.applier.appUserId = :appUserId AND p.leaveType = 'child_care' " +
            "AND FUNCTION('YEAR', p.requestDateTime) = FUNCTION('YEAR', CURRENT_DATE)")
    List<PtoEntity> findUserRequestsForChildCare(@Param("appUserId") Long appUserId);

    @Query("SELECT p FROM pto_requests p WHERE p.applier.appUserId = :appUserId AND p.leaveType = 'child_care' " +
            "AND YEAR(p.ptoStart) = :year")
    List<PtoEntity> findUserRequestsForChildCareAndYear(@Param("appUserId") Long appUserId, @Param("year") Integer year);


}
