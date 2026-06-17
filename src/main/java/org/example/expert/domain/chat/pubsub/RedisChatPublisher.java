package org.example.expert.domain.chat.pubsub;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;

import org.example.expert.domain.chat.dto.response.ChatMessageResponse;
import org.example.expert.domain.common.exception.ServerException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RedisChatPublisher {

	private final StringRedisTemplate stringRedisTemplate;
	private final ChannelTopic chatChannelTopic;
	private final ObjectMapper objectMapper;

	public void publish(ChatMessageResponse response) {
		try {
			String message = objectMapper.writeValueAsString(ChatMessageEvent.from(response));
			stringRedisTemplate.convertAndSend(chatChannelTopic.getTopic(), message);
		} catch (JsonProcessingException e) {
			throw new ServerException("Failed to publish chat message.");
		}
	}
}
