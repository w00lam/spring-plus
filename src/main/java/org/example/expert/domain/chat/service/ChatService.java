package org.example.expert.domain.chat.service;

import java.security.Principal;
import java.util.Comparator;
import java.util.List;

import org.example.expert.domain.chat.dto.request.ChatMessageRequest;
import org.example.expert.domain.chat.dto.response.ChatMessageResponse;
import org.example.expert.domain.chat.entity.ChatMessage;
import org.example.expert.domain.chat.repository.ChatMessageRepository;
import org.example.expert.domain.common.dto.AuthUser;
import org.example.expert.domain.common.dto.AuthenticatedUser;
import org.example.expert.domain.todo.entity.Todo;
import org.example.expert.domain.user.entity.User;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatService {

	private final ChatMessageRepository chatMessageRepository;
	private final ChatAccessValidator chatAccessValidator;

	@Transactional
	public ChatMessageResponse createMessage(Long todoId, ChatMessageRequest chatMessageRequest, Principal principal) {
		User sender = AuthenticatedUser.fromPrincipal(principal);

		Todo todo = chatAccessValidator.validateAccessible(todoId, sender.getId());

		ChatMessage chatMessage = new ChatMessage(
			sender.getNickname(),
			chatMessageRequest.getMessage(),
			todo);

		ChatMessage savedChatMessage = chatMessageRepository.save(chatMessage);

		return new ChatMessageResponse(
			savedChatMessage.getId(),
			todo.getId(),
			savedChatMessage.getSenderName(),
			savedChatMessage.getMessage(),
			savedChatMessage.getCreatedAt()
		);
	}

	public List<ChatMessageResponse> getMessages(Long todoId, Long cursor, int size, AuthUser authUser) {
		chatAccessValidator.validateAccessible(todoId, authUser.getId());

		Pageable pageable = PageRequest.of(0, size);

		List<ChatMessage> chatMessages = cursor == null
			? chatMessageRepository.findByTodoIdOrderByIdDesc(todoId, pageable)
			: chatMessageRepository.findByTodoIdAndIdLessThanOrderByIdDesc(todoId, cursor, pageable);

		return chatMessages.stream()
			.sorted(Comparator.comparing(ChatMessage::getId))
			.map(chatMessage -> new ChatMessageResponse(
				chatMessage.getId(),
				todoId,
				chatMessage.getSenderName(),
				chatMessage.getMessage(),
				chatMessage.getCreatedAt()
			))
			.toList();
	}
}
