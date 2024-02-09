package com.example.clocklike_portal.job_position;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PositionRepository extends JpaRepository<PositionEntity, Long> {
    Optional<PositionEntity> findByPositionKeyIgnoreCase(String name);
}
