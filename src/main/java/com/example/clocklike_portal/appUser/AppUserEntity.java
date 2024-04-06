package com.example.clocklike_portal.appUser;

import com.example.clocklike_portal.job_position.PositionEntity;
import com.example.clocklike_portal.job_position.PositionHistory;
import com.example.clocklike_portal.pto.PtoEntity;
import com.example.clocklike_portal.security.GooglePrincipal;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDate;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Data
@ToString
public class AppUserEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long appUserId;
    private String firstName;
    private String lastName;
    @Column(unique = true)
    private String userEmail;
    @ManyToMany(fetch = FetchType.EAGER)
    private Collection<UserRole> userRoles = new LinkedHashSet<>();
    private boolean isActive;
    private boolean isStillHired;
    @ManyToOne()
    @JoinColumn(name = "positionId")
    private PositionEntity position;
    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private Set<PositionHistory> positionHistory = new LinkedHashSet<>();
    private LocalDate hireStart;
    private LocalDate hireEnd;
    private int ptoDaysAccruedLastYear;
    private int ptoDaysAccruedCurrentYear;
    private int ptoDaysLeftFromLastYear;
    private int ptoDaysLeftCurrentYear;
    private int ptoDaysTaken;
    @OneToMany(mappedBy = "ptoRequestId", fetch = FetchType.LAZY)
    private Set<PtoEntity> ptoRequests = new LinkedHashSet<>();
    @OneToMany(mappedBy = "acceptor")
    private Set<PtoEntity> ptoAcceptor = new LinkedHashSet<>();

    public static AppUserEntity createTestAppUser(String firstName, String lastName, String userEmail) {
        return new AppUserEntity(
                null,
                firstName,
                lastName,
                userEmail,
                new LinkedHashSet<>(),
                false,
                true,
                null,
                new LinkedHashSet<>(),
                null,
                null,
                0,
                0,
                0,
                0,
                0,
                new LinkedHashSet<>(),
                new LinkedHashSet<>()
        );
    }


    public static AppUserEntity createUserFromGooglePrincipal(GooglePrincipal googlePrincipal) {
        return new AppUserEntity(
                null,
                googlePrincipal.getFirstName(),
                googlePrincipal.getLastName(),
                googlePrincipal.getEmail(),
                null,
                false,
                true,
                null,
                new LinkedHashSet<>(),
                null,
                null,
                0,
                0,
                0,
                0,
                0,
                new LinkedHashSet<>(),
                new LinkedHashSet<>()
        );
    }
}
