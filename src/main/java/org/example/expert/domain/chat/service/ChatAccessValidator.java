package org.example.expert.domain.chat.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.example.expert.domain.common.exception.InvalidRequestException;
import org.example.expert.domain.manager.repository.ManagerRepository;
import org.example.expert.domain.todo.entity.Todo;
import org.example.expert.domain.todo.repository.TodoRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChatAccessValidator {

	private final TodoRepository todoRepository;
	private final ManagerRepository managerRepository;

	@Transactional(readOnly = true)
	public Todo validateAccessible(Long todoId, Long userId) {
		Todo todo = todoRepository.findById(todoId)
			.orElseThrow(() -> new InvalidRequestException("Todo not found"));

		if (isOwner(todo, userId) || managerRepository.existsByTodoIdAndUserId(todoId, userId)) {
			return todo;
		}

		log.warn("Chat access denied. todoId={}, userId={}", todoId, userId);
		throw new InvalidRequestException("You do not have permission to access this chat room.");
	}

	private boolean isOwner(Todo todo, Long userId) {
		return todo.getUser() != null && ObjectUtils.nullSafeEquals(todo.getUser().getId(), userId);
	}
}
