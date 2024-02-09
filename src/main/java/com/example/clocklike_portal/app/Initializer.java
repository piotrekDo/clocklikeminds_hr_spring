package com.example.clocklike_portal.app;

import com.example.clocklike_portal.appUser.AppUserEntity;
import com.example.clocklike_portal.appUser.AppUserRepository;
import com.example.clocklike_portal.appUser.UserRole;
import com.example.clocklike_portal.appUser.UserRoleRepository;
import com.example.clocklike_portal.job_position.PositionEntity;
import com.example.clocklike_portal.job_position.PositionRepository;
import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.example.clocklike_portal.appUser.AppUserEntity.createTestAppUser;


@Component
@AllArgsConstructor
public class Initializer {
    private final UserRoleRepository userRoleRepository;
    private final PositionRepository positionRepository;
    private final AppUserRepository appUserRepository;

    @PostConstruct
    public void run() {
        UserRole userRole = userRoleRepository.save(new UserRole("user"));
        UserRole adminRole = userRoleRepository.save(new UserRole("admin"));

        PositionEntity ceoPosition = positionRepository.save(new PositionEntity("ceo", "CEO"));
        PositionEntity juniorJavaDeveloperPosition = positionRepository.save(new PositionEntity("junior_java_dev", "Junior Java Developer"));
        PositionEntity javaDeveloperPosition = positionRepository.save(new PositionEntity("java_dev", "Java Developer"));


        AppUserEntity admin = createTestAppUser("Admin", "Adminowski", "admin.adminowski@clocklikeminds.com");
        admin.setUserRoles(List.of(userRole, adminRole));
        admin.setPosition(ceoPosition);
        appUserRepository.save(admin);

        AppUserEntity piotrek = createTestAppUser("Piotr", "Domagalski", "piotr.domagalski@clocklikeminds.com");
        piotrek.setUserRoles(List.of(userRole, adminRole));
        appUserRepository.save(piotrek);

        AppUserEntity user1 = createTestAppUser("User", "Userski", "user.userski@clocklikeminds.com");
        user1.setUserRoles(List.of(userRole));
        appUserRepository.save(user1);

        AppUserEntity user2 = createTestAppUser("Userdrugi", "UserskiDrugi", "user2.userski2@clocklikeminds.com");
        user1.setUserRoles(List.of(userRole));
        appUserRepository.save(user2);

    }
}
