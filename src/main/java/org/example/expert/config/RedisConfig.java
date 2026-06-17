package org.example.expert.config;

import org.example.expert.domain.chat.pubsub.RedisChatSubscriber;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

@Configuration
public class RedisConfig {

	public static final String TODO_CHAT_CHANNEL = "todo.chat.messages";

	@Bean
	public ChannelTopic chatChannelTopic() {
		return new ChannelTopic(TODO_CHAT_CHANNEL);
	}

	@Bean
	public RedisMessageListenerContainer redisMessageListenerContainer(
		RedisConnectionFactory connectionFactory,
		RedisChatSubscriber redisChatSubscriber,
		ChannelTopic chatChannelTopic
	) {
		RedisMessageListenerContainer container = new RedisMessageListenerContainer();
		container.setConnectionFactory(connectionFactory);
		container.addMessageListener(redisChatSubscriber, chatChannelTopic);
		return container;
	}
}
