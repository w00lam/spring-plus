package org.example.expert.domain.log.service;

import lombok.RequiredArgsConstructor;
import org.example.expert.domain.log.entity.ManagerRegistrationLog;
import org.example.expert.domain.log.enums.ManagerRegistrationLogStatus;
import org.example.expert.domain.log.repository.ManagerRegistrationLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ManagerRegistrationLogService {

    private final ManagerRegistrationLogRepository managerRegistrationLogRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveLog(
            Long requestUserId,
            String requestUserEmail,
            Long todoId,
            Long managerUserId,
            String managerUserEmail,
            ManagerRegistrationLogStatus status,
            String errorMessage
    ) {
        ManagerRegistrationLog log = new ManagerRegistrationLog(
                requestUserId,
                requestUserEmail,
                todoId,
                managerUserId,
                managerUserEmail,
                status,
                errorMessage
        );

        managerRegistrationLogRepository.save(log);
    }
}
