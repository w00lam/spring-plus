package org.example.expert.domain.todo.dto.request;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class TodoSearchCondition {

	private String weather;

	@DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
	private LocalDate modifiedAtFrom;

	@DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
	private LocalDate modifiedAtTo;
}