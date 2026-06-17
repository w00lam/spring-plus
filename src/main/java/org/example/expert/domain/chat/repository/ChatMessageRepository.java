package org.example.expert.domain.chat.repository;

import java.util.List;

import org.example.expert.domain.chat.entity.ChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
	List<ChatMessage> findByTodoIdOrderByIdDesc(Long todoId, Pageable pageable);

	List<ChatMessage> findByTodoIdAndIdLessThanOrderByIdDesc(Long todoId, Long cursor, Pageable pageable);
}
