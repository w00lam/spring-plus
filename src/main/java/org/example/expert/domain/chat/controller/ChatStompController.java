package org.example.expert.domain.chat.controller;

import java.security.Principal;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.example.expert.domain.chat.dto.request.ChatMessageRequest;
import org.example.expert.domain.chat.dto.response.ChatMessageResponse;
import org.example.expert.domain.chat.pubsub.RedisChatPublisher;
import org.example.expert.domain.chat.service.ChatService;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class ChatStompController {

	private final ChatService chatService;
	private final RedisChatPublisher redisChatPublisher;

	@MessageMapping("/todos/{todoId}/chat")
	public void sendMessage(
		@DestinationVariable Long todoId,
		@Valid @Payload ChatMessageRequest request,
		Principal principal
	) {
		ChatMessageResponse response = chatService.createMessage(todoId, request, principal);
		redisChatPublisher.publish(response);
	}
}
