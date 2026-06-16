package org.example.expert.domain.log.repository;

import org.example.expert.domain.log.entity.ManagerRegistrationLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ManagerRegistrationLogRepository extends JpaRepository<ManagerRegistrationLog, Long> {
}
