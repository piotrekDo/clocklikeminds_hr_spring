package com.example.clocklike_portal.app;

import com.example.clocklike_portal.appUser.AppUserEntity;
import com.example.clocklike_portal.appUser.AppUserRepository;
import com.example.clocklike_portal.appUser.UserRole;
import com.example.clocklike_portal.appUser.UserRoleRepository;
import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;


@Component
@AllArgsConstructor
public class Initializer {
    private final UserRoleRepository userRoleRepository;
    private final AppUserRepository appUserRepository;

    @PostConstruct
    public void run() {
        UserRole userRole = userRoleRepository.save(new UserRole("user"));
        UserRole adminRole = userRoleRepository.save(new UserRole("admin"));

//        AppUserEntity piotrek =
//                appUserRepository.save(new AppUserEntity(null, "Piotr", "Domagalski",
//                        "piotr.domagalski@clocklikeminds.com", List.of(userRole, adminRole)));
    }
}
