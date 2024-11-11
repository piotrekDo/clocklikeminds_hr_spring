package com.example.clocklike_portal.timeoff_history;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RequestHistoryRepository extends JpaRepository<RequestHistory, Long> {
}
