package com.example.clocklike_portal.appUser;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AppUserRepository extends JpaRepository<AppUserEntity, Long> {
    Optional<AppUserEntity> findByUserEmailIgnoreCase(String userEmail);

    List<AppUserEntity> findAllByUserRolesContaining(UserRole role);

}
