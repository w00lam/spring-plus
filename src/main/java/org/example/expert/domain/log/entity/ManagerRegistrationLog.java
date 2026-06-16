package org.example.expert.domain.log.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.expert.domain.log.enums.ManagerRegistrationLogStatus;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@Entity
@NoArgsConstructor
@Table(name = "logs")
@EntityListeners(AuditingEntityListener.class)
public class ManagerRegistrationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long requestUserId;

    @Column(nullable = false)
    private String requestUserEmail;

    @Column(nullable = false)
    private Long todoId;

    @Column
    private Long managerUserId;

    @Column
    private String managerUserEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ManagerRegistrationLogStatus status;

    @Column(length = 1000)
    private String errorMessage;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public ManagerRegistrationLog(
            Long requestUserId,
            String requestUserEmail,
            Long todoId,
            Long managerUserId,
            String managerUserEmail,
            ManagerRegistrationLogStatus status,
            String errorMessage
    ) {
        this.requestUserId = requestUserId;
        this.requestUserEmail = requestUserEmail;
        this.todoId = todoId;
        this.managerUserId = managerUserId;
        this.managerUserEmail = managerUserEmail;
        this.status = status;
        this.errorMessage = errorMessage;
    }
}
