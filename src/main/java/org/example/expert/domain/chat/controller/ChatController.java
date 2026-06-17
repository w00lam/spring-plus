package org.example.expert.domain.chat.controller;

import java.util.List;

import org.example.expert.domain.chat.dto.response.ChatMessageResponse;
import org.example.expert.domain.chat.service.ChatService;
import org.example.expert.domain.common.dto.AuthUser;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class ChatController {

	private final ChatService chatService;

	@GetMapping("/todos/{todoId}/chat/messages")
	public ResponseEntity<List<ChatMessageResponse>> findByTodoId(
		@PathVariable Long todoId,
		@RequestParam(required = false) Long cursor,
		@RequestParam(defaultValue = "20") int size,
		@AuthenticationPrincipal AuthUser authUser
	) {
		return ResponseEntity.ok(chatService.getMessages(todoId, cursor, size, authUser));
	}
}
