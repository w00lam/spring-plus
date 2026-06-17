package org.example.expert.config;

import java.security.Principal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.example.expert.domain.chat.service.ChatAccessValidator;
import org.example.expert.domain.common.dto.AuthenticatedUser;
import org.example.expert.domain.common.exception.InvalidRequestException;
import org.example.expert.domain.user.entity.User;
import org.example.expert.domain.user.repository.UserRepository;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class StompAuthInterceptor implements ChannelInterceptor {

	private static final Pattern TODO_CHAT_TOPIC_PATTERN = Pattern.compile("^/topic/todos/(\\d+)/chat$");

	private final JwtUtil jwtUtil;
	private final UserRepository userRepository;
	private final ChatAccessValidator chatAccessValidator;

	@Override
	public Message<?> preSend(Message<?> message, MessageChannel channel) {
		StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

		if (StompCommand.CONNECT.equals(accessor.getCommand())) {
			String bearerToken = accessor.getFirstNativeHeader("Authorization");
			String token = jwtUtil.substringToken(bearerToken);

			Claims claims = jwtUtil.extractClaims(token);
			Long userId = Long.parseLong(claims.getSubject());

			User user = userRepository.findById(userId)
				.orElseThrow(() -> new InvalidRequestException("User not found"));

			accessor.setUser(new AuthenticatedUser(user));
			log.debug("STOMP CONNECT authenticated. userId={}", userId);
		}

		if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
			validateSubscribeDestination(accessor);
		}

		return message;
	}

	private void validateSubscribeDestination(StompHeaderAccessor accessor) {
		String destination = accessor.getDestination();
		if (destination == null) {
			return;
		}

		Matcher matcher = TODO_CHAT_TOPIC_PATTERN.matcher(destination);
		if (!matcher.matches()) {
			return;
		}

		Principal principal = accessor.getUser();
		if (principal == null) {
			throw new InvalidRequestException("Authentication is required.");
		}

		Long todoId = Long.parseLong(matcher.group(1));
		User user = AuthenticatedUser.fromPrincipal(principal);
		chatAccessValidator.validateAccessible(todoId, user.getId());
		log.debug("STOMP SUBSCRIBE authorized. destination={}, todoId={}, userId={}", destination, todoId, user.getId());
	}
}
