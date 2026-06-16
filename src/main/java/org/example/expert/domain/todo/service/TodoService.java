package org.example.expert.domain.todo.service;

import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.RequiredArgsConstructor;
import org.example.expert.client.WeatherClient;
import org.example.expert.domain.common.dto.AuthUser;
import org.example.expert.domain.common.exception.InvalidRequestException;
import org.example.expert.domain.todo.dto.request.TodoSaveRequest;
import org.example.expert.domain.todo.dto.request.TodoSearchCondition;
import org.example.expert.domain.todo.dto.response.TodoResponse;
import org.example.expert.domain.todo.dto.response.TodoSaveResponse;
import org.example.expert.domain.todo.entity.Todo;
import org.example.expert.domain.todo.repository.TodoRepository;
import org.example.expert.domain.user.dto.response.UserResponse;
import org.example.expert.domain.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TodoService {

    private final TodoRepository todoRepository;
    private final WeatherClient weatherClient;

    @Transactional
    public TodoSaveResponse saveTodo(AuthUser authUser, TodoSaveRequest todoSaveRequest) {
        User user = User.fromAuthUser(authUser);

        String weather = weatherClient.getTodayWeather();

        Todo newTodo = new Todo(
                todoSaveRequest.getTitle(),
                todoSaveRequest.getContents(),
                weather,
                user
        );
        Todo savedTodo = todoRepository.save(newTodo);

        return new TodoSaveResponse(
                savedTodo.getId(),
                savedTodo.getTitle(),
                savedTodo.getContents(),
                weather,
                new UserResponse(user.getId(), user.getEmail())
        );
    }

    public Page<TodoResponse> getTodos(TodoSearchCondition condition, int page, int size) {
        validatePageRequest(page, size);
        validateModifiedAtRange(condition);

        Pageable pageable = PageRequest.of(page - 1, size);


        Page<Todo> todos = todoRepository.searchTodosWithCondition(
            normalizeWeather(condition.getWeather()),
            toStartDateTime(condition.getModifiedAtFrom()),
            toExclusiveEndDateTime(condition.getModifiedAtTo()),
            pageable
        );

        return todos.map(TodoResponse::from);
    }

    public TodoResponse getTodo(long todoId) {
        Todo todo = todoRepository.findByIdWithUser(todoId)
                .orElseThrow(() -> new InvalidRequestException("Todo not found"));

        User user = todo.getUser();

        return new TodoResponse(
                todo.getId(),
                todo.getTitle(),
                todo.getContents(),
                todo.getWeather(),
                new UserResponse(user.getId(), user.getEmail()),
                todo.getCreatedAt(),
                todo.getModifiedAt()
        );
    }

    private void validatePageRequest(int page, int size) {
        if (page < 1) {
            throw new InvalidRequestException("페이지 번호는 1 이상이어야 합니다.");
        }

        if (size < 1) {
            throw new InvalidRequestException("페이지 크기는 1 이상이어야 합니다.");
        }
    }

    private void validateModifiedAtRange(TodoSearchCondition condition) {
        if (condition.getModifiedAtFrom() != null
            && condition.getModifiedAtTo() != null
            && condition.getModifiedAtFrom().isAfter(condition.getModifiedAtTo())) {
            throw new InvalidRequestException("수정일 시작일은 종료일보다 이후일 수 없습니다.");
        }
    }

    private String normalizeWeather(String weather) {
        return StringUtils.hasText(weather) ? weather : null;
    }

    private LocalDateTime toStartDateTime(LocalDate date) {
        return date == null ? null : date.atStartOfDay();
    }

    private LocalDateTime toExclusiveEndDateTime(LocalDate date) {
        return date == null ? null : date.plusDays(1).atStartOfDay();
    }
}
