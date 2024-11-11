package com.example.clocklike_portal.timeoff;


import com.example.clocklike_portal.app.Library;
import com.example.clocklike_portal.appUser.AppUserEntity;
import com.example.clocklike_portal.timeoff_history.RequestHistory;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
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
    private boolean wasMarkedToWithdraw = false;
    private boolean wasWithdrawn = false;
    private LocalDateTime withdrawnDateTime = null;
    @OneToMany(fetch = FetchType.EAGER, mappedBy = "ptoEntity", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RequestHistory> history = new ArrayList<>();
    private String declineReason;

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
                '}';
    }

    static class Action {
        public static String REGISTER = "REGISTER";
        public static String ACCEPTED = "ACCEPTED";
        public static String DECLINED = "DECLINED";
        public static String MARKED_WITHDRAW = "MARKED_WITHDRAW";
        public static String WITHDRAW = "WITHDRAW";
        public static String WITHDRAW_DECLINED = "WITHDRAW_DECLINED";
    }

}


