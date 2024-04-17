package com.example.clocklike_portal.app;

import com.example.clocklike_portal.appUser.AppUserEntity;
import com.example.clocklike_portal.appUser.AppUserRepository;
import com.example.clocklike_portal.appUser.UserRole;
import com.example.clocklike_portal.appUser.UserRoleRepository;
import com.example.clocklike_portal.job_position.PositionEntity;
import com.example.clocklike_portal.job_position.PositionRepository;
import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import org.springframework.core.annotation.Order;
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
        UserRole supervisorRole = userRoleRepository.save(new UserRole("supervisor"));

        PositionEntity ceoPosition = positionRepository.save(new PositionEntity("ceo", "CEO"));
        PositionEntity boardMemberPosition = positionRepository.save(new PositionEntity("board_member", "Board Member"));
        PositionEntity projectManagerPosition = positionRepository.save(new PositionEntity("project_manager", "Project Manager"));
        PositionEntity pegaCertBusinessArchPosition = positionRepository.save(new PositionEntity("pega_cert_business_architect", "Pega Certified Business Architect"));
        PositionEntity pegaLeadArchPosition = positionRepository.save(new PositionEntity("pega_cert_lead_architect", "Pega Certified Lead System Architect"));
        PositionEntity pegaCertSeniorPosition = positionRepository.save(new PositionEntity("pega_cert_senior_architect", "Pega Certified Senior System Architect"));
        PositionEntity pegaSeniorArchPosition = positionRepository.save(new PositionEntity("pega_senior_architect", "Pega Senior System Architect"));
        PositionEntity pegaCertArchitect = positionRepository.save(new PositionEntity("pega_cert_architect", "Pega Certified System Architect"));
        PositionEntity juniorJavaDeveloperPosition = positionRepository.save(new PositionEntity("junior_java_dev", "Junior Java Developer"));
        PositionEntity javaDeveloperPosition = positionRepository.save(new PositionEntity("java_dev", "Java Developer"));


        AppUserEntity admin = createTestAppUser("Admin", "Adminowski", "admin.adminowski@clocklikeminds.com");
        admin.setUserRoles(List.of(userRole, adminRole, supervisorRole));
        admin.setPtoDaysAccruedLastYear(2);
        admin.setPtoDaysAccruedCurrentYear(26);
        admin.setPtoDaysLeftFromLastYear(2);
        admin.setPtoDaysLeftCurrentYear(26);
        admin.setRegistrationFinished(true);
        admin.setActive(true);
        admin.setPosition(ceoPosition);
        appUserRepository.save(admin);

        AppUserEntity piotrek = createTestAppUser("Piotr", "Domagalski", "piotr.domagalski@clocklikeminds.com");
        piotrek.setUserRoles(List.of(userRole, adminRole, supervisorRole));
        piotrek.setPtoDaysAccruedLastYear(2);
        piotrek.setPtoDaysAccruedCurrentYear(26);
        piotrek.setPtoDaysLeftFromLastYear(2);
        piotrek.setPtoDaysLeftCurrentYear(26);
        piotrek.setRegistrationFinished(true);
        piotrek.setActive(true);
        appUserRepository.save(piotrek);

        AppUserEntity user1 = createTestAppUser("User", "Userski", "user.userski@clocklikeminds.com");
        user1.setUserRoles(List.of(userRole));
        appUserRepository.save(user1);

        AppUserEntity user2 = createTestAppUser("Userdrugi", "UserskiDrugi", "user2.userski2@clocklikeminds.com");
        user2.setUserRoles(List.of(userRole));
        appUserRepository.save(user2);

    }
}
