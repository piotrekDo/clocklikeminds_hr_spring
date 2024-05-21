package com.example.clocklike_portal.appUser;

import com.example.clocklike_portal.job_position.PositionEntity;
import com.example.clocklike_portal.job_position.PositionHistory;
import com.example.clocklike_portal.pto.HolidayOnSaturdayUserEntity;
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
import java.util.Objects;
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
    private String imageUrl;
    @ManyToMany(fetch = FetchType.EAGER)
    private Collection<UserRole> userRoles = new LinkedHashSet<>();
    private boolean isRegistrationFinished;
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
    @ManyToOne
    private AppUserEntity supervisor;
    @OneToMany(mappedBy = "supervisor")
    private Set<AppUserEntity> subordinates;
    @OneToMany(mappedBy = "ptoRequestId", fetch = FetchType.LAZY)
    private Set<PtoEntity> ptoRequests = new LinkedHashSet<>();
    @OneToMany(mappedBy = "acceptor")
    private Set<PtoEntity> ptoAcceptor = new LinkedHashSet<>();
    @OneToMany(mappedBy = "user")
    private Set<HolidayOnSaturdayUserEntity> holidaysOnSaturday;

    public static AppUserEntity createTestAppUser(String firstName, String lastName, String userEmail) {
        return new AppUserEntity(
                null,
                firstName,
                lastName,
                userEmail,
                null,
                new LinkedHashSet<>(),
                false,
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
                null,
                new LinkedHashSet<>(),
                new LinkedHashSet<>(),
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
                googlePrincipal.getPictureUrl(),
                null,
                false,
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
                null,
                new LinkedHashSet<>(),
                new LinkedHashSet<>(),
                new LinkedHashSet<>(),
                new LinkedHashSet<>()
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AppUserEntity that = (AppUserEntity) o;
        return Objects.equals(appUserId, that.appUserId) && Objects.equals(firstName, that.firstName) && Objects.equals(lastName, that.lastName) && Objects.equals(userEmail, that.userEmail);
    }

    @Override
    public int hashCode() {
        return Objects.hash(appUserId, firstName, lastName, userEmail);
    }
}
