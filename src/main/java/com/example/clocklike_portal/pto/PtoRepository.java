package com.example.clocklike_portal.pto;

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

    List<PtoEntity> findAllByDecisionDateTimeIsNullAndAcceptor_appUserId(long id);

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


    List<PtoEntity> findAllByApplierAndPtoStartLessThanEqualAndPtoEndGreaterThanEqualAndDecisionDateTimeIsNotNullAndWasAcceptedIsTrue(AppUserEntity applier, LocalDate ptoEnd, LocalDate ptoStart);
}
