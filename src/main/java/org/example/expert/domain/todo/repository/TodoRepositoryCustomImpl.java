package org.example.expert.domain.todo.repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.example.expert.domain.comment.entity.QComment;
import org.example.expert.domain.manager.entity.QManager;
import org.example.expert.domain.todo.dto.request.TodoSearchCondition;
import org.example.expert.domain.todo.dto.response.TodoSearchResponse;
import org.example.expert.domain.todo.entity.QTodo;
import org.example.expert.domain.todo.entity.Todo;
import org.example.expert.domain.user.entity.QUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.util.StringUtils;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class TodoRepositoryCustomImpl implements TodoRepositoryCustom {

	private final JPAQueryFactory jpaQueryFactory;

	@Override
	public Optional<Todo> findByIdWithUser(Long todoId) {
		QTodo todo = QTodo.todo;
		QUser user = QUser.user;

		Todo result = jpaQueryFactory
			.selectFrom(todo)
			.join(todo.user, user).fetchJoin()
			.where(todo.id.eq(todoId))
			.fetchOne();

		return Optional.ofNullable(result);
	}

	@Override
	public Page<TodoSearchResponse> searchTodos(TodoSearchCondition condition, Pageable pageable) {
		QTodo todo = QTodo.todo;
		QManager manager = QManager.manager;
		QComment comment = QComment.comment;

		BooleanBuilder where = buildSearchCondition(condition);
		NumberExpression<Long> managerCount = manager.id.countDistinct();
		NumberExpression<Long> commentCount = comment.id.countDistinct();

		List<TodoSearchResponse> content = jpaQueryFactory
			.select(Projections.constructor(
				TodoSearchResponse.class,
				todo.title,
				managerCount,
				commentCount
			))
			.from(todo)
			.leftJoin(todo.managers, manager)
			.leftJoin(todo.comments, comment)
			.where(where)
			.groupBy(todo.id, todo.title, todo.createdAt)
			.orderBy(todo.createdAt.desc())
			.offset(pageable.getOffset())
			.limit(pageable.getPageSize())
			.fetch();

		Long total = jpaQueryFactory
			.select(todo.id.countDistinct())
			.from(todo)
			.where(where)
			.fetchOne();

		return PageableExecutionUtils.getPage(content, pageable, () -> total == null ? 0L : total);
	}

	private BooleanBuilder buildSearchCondition(TodoSearchCondition condition) {
		QTodo todo = QTodo.todo;

		BooleanBuilder builder = new BooleanBuilder();

		if (StringUtils.hasText(condition.getKeyword())) {
			builder.and(todo.title.contains(condition.getKeyword()));
		}

		if (condition.getCreatedAtFrom() != null) {
			builder.and(todo.createdAt.goe(toStartDateTime(condition.getCreatedAtFrom())));
		}

		if (condition.getCreatedAtTo() != null) {
			builder.and(todo.createdAt.lt(toExclusiveEndDateTime(condition.getCreatedAtTo())));
		}

		if (StringUtils.hasText(condition.getManagerNickname())) {
			builder.and(todo.managers.any().user.nickname.containsIgnoreCase(condition.getManagerNickname()));
		}

		return builder;
	}

	private LocalDateTime toStartDateTime(LocalDate date) {
		return date == null ? null : date.atStartOfDay();
	}

	private LocalDateTime toExclusiveEndDateTime(LocalDate date) {
		return date == null ? null : date.plusDays(1).atStartOfDay();
	}
}
