package com.example.clocklike_portal.appUser;

import com.example.clocklike_portal.security.GooglePrincipal;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.Collection;
import java.util.LinkedHashSet;

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

    public static AppUserEntity createUserFromGooglePrincipal(GooglePrincipal googlePrincipal) {
        return new AppUserEntity(
                null,
                googlePrincipal.getFirstName(),
                googlePrincipal.getLastName(),
                googlePrincipal.getEmail(),
                null
        );
    }
}
