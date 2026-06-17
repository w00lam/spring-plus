package org.example.expert.domain.chat.dto.response;

import java.time.LocalDateTime;

import lombok.Getter;

@Getter
public class ChatMessageResponse {

	private final Long messageId;
	private final Long todoId;
	private final String senderName;
	private final String message;
	private final LocalDateTime sentAt;

	public ChatMessageResponse(Long messageId, Long todoId, String senderName, String message, LocalDateTime sentAt) {
		this.messageId = messageId;
		this.todoId = todoId;
		this.senderName = senderName;
		this.message = message;
		this.sentAt = sentAt;
	}
}
