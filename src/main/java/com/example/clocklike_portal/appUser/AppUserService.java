package com.example.clocklike_portal.appUser;

import com.example.clocklike_portal.security.GooglePrincipal;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@AllArgsConstructor
public class AppUserService {

    private final AppUserRepository appUserRepository;
    private final UserRoleRepository userRoleRepository;

    public AppUserEntity registerNewUser(GooglePrincipal googlePrincipal) {
        AppUserEntity userFromGooglePrincipal = AppUserEntity.createUserFromGooglePrincipal(googlePrincipal);
        UserRole userRole = userRoleRepository.findByRoleNameIgnoreCase("user")
                .orElseThrow(() -> new NoSuchElementException("No user role found"));
        userFromGooglePrincipal.setUserRoles(List.of(userRole));
        return appUserRepository.save(userFromGooglePrincipal);
    }
}
