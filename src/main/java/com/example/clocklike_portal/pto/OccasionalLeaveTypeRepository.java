package com.example.clocklike_portal.pto;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OccasionalLeaveTypeRepository extends JpaRepository<OccasionalLeaveType, Integer> {
}
