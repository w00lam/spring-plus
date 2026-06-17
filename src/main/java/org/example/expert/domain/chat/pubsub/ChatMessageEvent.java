package org.example.expert.domain.chat.pubsub;

import java.time.LocalDateTime;

import org.example.expert.domain.chat.dto.response.ChatMessageResponse;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageEvent {

	private Long messageId;
	private Long todoId;
	private String senderName;
	private String message;
	private LocalDateTime sentAt;

	public static ChatMessageEvent from(ChatMessageResponse response) {
		return new ChatMessageEvent(
			response.getMessageId(),
			response.getTodoId(),
			response.getSenderName(),
			response.getMessage(),
			response.getSentAt()
		);
	}

	public ChatMessageResponse toResponse() {
		return new ChatMessageResponse(messageId, todoId, senderName, message, sentAt);
	}
}
