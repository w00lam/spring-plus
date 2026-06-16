package org.example.expert.aop;

import lombok.RequiredArgsConstructor;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.example.expert.domain.common.dto.AuthUser;
import org.example.expert.domain.log.enums.ManagerRegistrationLogStatus;
import org.example.expert.domain.log.service.ManagerRegistrationLogService;
import org.example.expert.domain.manager.dto.request.ManagerSaveRequest;
import org.example.expert.domain.user.entity.User;
import org.example.expert.domain.user.repository.UserRepository;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
@Order(Ordered.LOWEST_PRECEDENCE - 1)
public class ManagerRegistrationLoggingAspect {

	private final ManagerRegistrationLogService managerRegistrationLogService;
	private final UserRepository userRepository;

	@Around("execution(* org.example.expert.domain.manager.service.ManagerService.saveManager(..))")
	public Object logManagerRegistration(ProceedingJoinPoint joinPoint) throws Throwable {
		Object[] args = joinPoint.getArgs();
		AuthUser authUser = (AuthUser)args[0];
		Long todoId = (Long)args[1];
		ManagerSaveRequest managerSaveRequest = (ManagerSaveRequest)args[2];

		Long managerUserId = managerSaveRequest == null ? null : managerSaveRequest.getManagerUserId();
		String managerUserEmail = findManagerUserEmail(managerUserId);

		try {
			Object result = joinPoint.proceed();
			saveLog(authUser, todoId, managerUserId, managerUserEmail, ManagerRegistrationLogStatus.SUCCESS, null);
			return result;
		} catch (Throwable throwable) {
			saveLog(
				authUser,
				todoId,
				managerUserId,
				managerUserEmail,
				ManagerRegistrationLogStatus.FAILURE,
				truncateErrorMessage(throwable.getMessage())
			);
			throw throwable;
		}
	}

	private String findManagerUserEmail(Long managerUserId) {
		if (managerUserId == null) {
			return null;
		}

		return userRepository.findById(managerUserId)
			.map(User::getEmail)
			.orElse(null);
	}

	private void saveLog(
		AuthUser authUser,
		Long todoId,
		Long managerUserId,
		String managerUserEmail,
		ManagerRegistrationLogStatus status,
		String errorMessage
	) {
		managerRegistrationLogService.saveLog(
			authUser.getId(),
			authUser.getEmail(),
			todoId,
			managerUserId,
			managerUserEmail,
			status,
			errorMessage
		);
	}

	private String truncateErrorMessage(String errorMessage) {
		if (errorMessage == null || errorMessage.length() <= 1000) {
			return errorMessage;
		}

		return errorMessage.substring(0, 1000);
	}
}
