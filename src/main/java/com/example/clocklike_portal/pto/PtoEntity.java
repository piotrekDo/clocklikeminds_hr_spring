package com.example.clocklike_portal.pto;


import com.example.clocklike_portal.appUser.AppUserEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity(name = "pto_requests")
@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class PtoEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long ptoRequestId;
    private LocalDateTime requestDateTime;
    private LocalDate ptoStart;
    private LocalDate ptoEnd;
    @ManyToOne()
    private AppUserEntity applier;
    @ManyToOne
    private AppUserEntity acceptor;
    private boolean wasAccepted;
    private LocalDateTime decisionDateTime;
    private int businessDays;
    private int includingLastYearPool;
    private String declineReason;

    PtoEntity(LocalDate ptoStart, LocalDate ptoEnd, AppUserEntity applier, AppUserEntity acceptor, int businessDays) {
        this.requestDateTime = LocalDateTime.now();
        this.ptoStart = ptoStart;
        this.ptoEnd = ptoEnd;
        this.applier = applier;
        this.acceptor = acceptor;
        this.businessDays = businessDays;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PtoEntity ptoEntity = (PtoEntity) o;
        return Objects.equals(ptoRequestId, ptoEntity.ptoRequestId) && Objects.equals(requestDateTime, ptoEntity.requestDateTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ptoRequestId, requestDateTime);
    }

    @Override
    public String toString() {
        return "PtoEntity{" +
                "ptoRequestId=" + ptoRequestId +
                ", requestDateTime=" + requestDateTime +
                ", ptoStart=" + ptoStart +
                ", ptoEnd=" + ptoEnd +
                ", applier=" + applier.getAppUserId() +
                ", acceptor=" + acceptor.getAppUserId() +
                ", wasAccepted=" + wasAccepted +
                ", decisionDateTime=" + decisionDateTime +
                ", businessDays=" + businessDays +
                ", includingLastYearPool=" + includingLastYearPool +
                ", declineReason='" + declineReason + '\'' +
                '}';
    }

    //    @Override
//    public boolean equals(Object o) {
//        if (this == o) return true;
//        if (o == null || getClass() != o.getClass()) return false;
//        PtoEntity ptoEntity = (PtoEntity) o;
//        return wasAccepted == ptoEntity.wasAccepted && businessDays == ptoEntity.businessDays && includingLastYearPool == ptoEntity.includingLastYearPool && Objects.equals(ptoRequestId, ptoEntity.ptoRequestId) && Objects.equals(requestDateTime, ptoEntity.requestDateTime) && Objects.equals(ptoStart, ptoEntity.ptoStart) && Objects.equals(ptoEnd, ptoEntity.ptoEnd) && Objects.equals(applier.getAppUserId(), ptoEntity.applier.getAppUserId()) && Objects.equals(acceptor.getAppUserId(), ptoEntity.acceptor.getAppUserId()) && Objects.equals(decisionDateTime, ptoEntity.decisionDateTime) && Objects.equals(declineReason, ptoEntity.declineReason);
//    }
//
//    @Override
//    public int hashCode() {
//        return Objects.hash(ptoRequestId, requestDateTime, ptoStart, ptoEnd, applier.getAppUserId(), acceptor.getAppUserId(), wasAccepted, decisionDateTime, businessDays, includingLastYearPool, declineReason);
//    }
}
