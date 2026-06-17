package org.example.expert.domain.chat.entity;

import org.example.expert.domain.common.entity.Timestamped;
import org.example.expert.domain.todo.entity.Todo;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "chat_messages")
public class ChatMessage extends Timestamped {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String senderName;

	private String message;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "todo_id", nullable = false)
	private Todo todo;

	public ChatMessage(String senderName, String message, Todo todo) {
		this.senderName = senderName;
		this.message = message;
		this.todo = todo;
	}
}
