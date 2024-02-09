package com.example.clocklike_portal.job_position;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PositionHistoryRepository extends JpaRepository<PositionHistory, Long> {

}
