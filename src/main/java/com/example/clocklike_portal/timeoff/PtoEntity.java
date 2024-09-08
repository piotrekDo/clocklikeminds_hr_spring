package com.example.clocklike_portal.timeoff;


import com.example.clocklike_portal.app.Library;
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
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "discriminator_type", discriminatorType = DiscriminatorType.STRING)
@DiscriminatorValue(PtoEntity.DISCRIMINATOR_VALUE)
@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class PtoEntity {
    static final String DISCRIMINATOR_VALUE = Library.PTO_DISCRIMINATOR_VALUE;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long ptoRequestId;
    private String leaveType = Library.PTO_DISCRIMINATOR_VALUE;
    private boolean isDemand;
    private String applierNotes;
    private String acceptorNotes;
    private String applicationNotes;
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
    private boolean wasMarkedToWithdraw = false;
    private boolean wasWithdrawn = false;
    private LocalDateTime withdrawnDateTime = null;

    public PtoEntity(boolean isDemand, LocalDate ptoStart, LocalDate ptoEnd, AppUserEntity applier, AppUserEntity acceptor, int businessDays, int includingLastYearPool) {
        this.isDemand = isDemand;
        this.requestDateTime = LocalDateTime.now();
        this.ptoStart = ptoStart;
        this.ptoEnd = ptoEnd;
        this.applier = applier;
        this.acceptor = acceptor;
        this.businessDays = businessDays;
        this.includingLastYearPool = includingLastYearPool;
    }

    public PtoEntity(String leaveType, boolean isDemand, LocalDate ptoStart, LocalDate ptoEnd, AppUserEntity applier, AppUserEntity acceptor, int businessDays, int includingLastYearPool) {
        this.leaveType = leaveType;
        this.isDemand = isDemand;
        this.requestDateTime = LocalDateTime.now();
        this.ptoStart = ptoStart;
        this.ptoEnd = ptoEnd;
        this.applier = applier;
        this.acceptor = acceptor;
        this.businessDays = businessDays;
        this.includingLastYearPool = includingLastYearPool;
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
}
