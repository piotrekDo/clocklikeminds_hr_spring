package com.example.clocklike_portal.app;

import com.example.clocklike_portal.appUser.AppUserEntity;
import com.example.clocklike_portal.appUser.AppUserRepository;
import com.example.clocklike_portal.appUser.UserRole;
import com.example.clocklike_portal.appUser.UserRoleRepository;
import com.example.clocklike_portal.job_position.PositionEntity;
import com.example.clocklike_portal.job_position.PositionRepository;
import com.example.clocklike_portal.pto.OccasionalLeaveEntity;
import com.example.clocklike_portal.pto.OccasionalLeaveRepository;
import com.example.clocklike_portal.pto.OccasionalLeaveType;
import com.example.clocklike_portal.pto.OccasionalLeaveTypeRepository;
import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

import static com.example.clocklike_portal.appUser.AppUserEntity.createTestAppUser;


@Component
@AllArgsConstructor
public class Initializer {
    private final UserRoleRepository userRoleRepository;
    private final PositionRepository positionRepository;
    private final AppUserRepository appUserRepository;
    private final OccasionalLeaveTypeRepository occasionalLeaveTypeRepository;
    private final OccasionalLeaveRepository occasionalLeaveRepository;

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

        OccasionalLeaveType weddingOccasionalType = occasionalLeaveTypeRepository.save(new OccasionalLeaveType("wedding", "ślub pracownika (własny)", 2));
        OccasionalLeaveType birthOccasionalLeave = occasionalLeaveTypeRepository.save(new OccasionalLeaveType("birth", "narodziny dziecka u pracownika ", 2));
        OccasionalLeaveType parentFuneralOccasionalLeave = occasionalLeaveTypeRepository.save(new OccasionalLeaveType("parent_funeral", "śmierć, pogrzeb matki lub ojca / ojczyma lub macochy", 2));
        OccasionalLeaveType spouseFuneralOccasionalLeave = occasionalLeaveTypeRepository.save(new OccasionalLeaveType("spouse_funeral", "śmierć, pogrzeb męża lub żony ", 2));
        OccasionalLeaveType childFuneralOccasionalLeave = occasionalLeaveTypeRepository.save(new OccasionalLeaveType("child_funeral", "śmierć lub pogrzeb dziecka", 2));
        OccasionalLeaveType childWeddingOccasionalLeave = occasionalLeaveTypeRepository.save(new OccasionalLeaveType("child_wedding", "ślub dziecka ", 1));
        OccasionalLeaveType siblingFuneralOccasionalLeave = occasionalLeaveTypeRepository.save(new OccasionalLeaveType("sibling_funeral", "śmierć lub pogrzeb brata, siostry", 1));
        OccasionalLeaveType inLawFuneralOccasionalLeave = occasionalLeaveTypeRepository.save(new OccasionalLeaveType("in_law_funeral", "śmierć lub pogrzeb teściowej, teścia", 1));
        OccasionalLeaveType grandparentFuneralOccasionalLeave = occasionalLeaveTypeRepository.save(new OccasionalLeaveType("grandparent_funeral", "śmierć lub pogrzeb babki, dziadka", 1));
        OccasionalLeaveType dependentFuneralOccasionalLeave = occasionalLeaveTypeRepository.save(new OccasionalLeaveType("dependent_funeral", "śmierć lub pogrzeb osób, które pozostawały na utrzymaniu lub pod opieką pracownika", 1));
        OccasionalLeaveType childCareOccasionalLeave = occasionalLeaveTypeRepository.save(new OccasionalLeaveType("child_care", "opieka nad dzieckiem", 2));

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

        piotrek.setSupervisor(piotrek);
        admin.setSupervisor(piotrek);
        appUserRepository.save(piotrek);
        appUserRepository.save(admin);

        OccasionalLeaveEntity takiTamTest = new OccasionalLeaveEntity(LocalDate.now(), LocalDate.now(), piotrek, piotrek, 2, weddingOccasionalType);
        occasionalLeaveRepository.save(takiTamTest);
    }
}
