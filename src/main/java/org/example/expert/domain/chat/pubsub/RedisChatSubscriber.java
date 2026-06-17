package org.example.expert.domain.chat.pubsub;

import java.nio.charset.StandardCharsets;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisChatSubscriber implements MessageListener {

	private final ObjectMapper objectMapper;
	private final SimpMessagingTemplate messagingTemplate;

	@Override
	public void onMessage(Message message, byte[] pattern) {
		try {
			String payload = new String(message.getBody(), StandardCharsets.UTF_8);
			ChatMessageEvent event = objectMapper.readValue(payload, ChatMessageEvent.class);
			messagingTemplate.convertAndSend("/topic/todos/" + event.getTodoId() + "/chat", event.toResponse());
		} catch (JsonProcessingException e) {
			log.error("Failed to deserialize chat message from Redis.", e);
		}
	}
}
