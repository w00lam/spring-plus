package org.example.expert.domain.user.dto.response;

import org.example.expert.domain.user.entity.User;

import lombok.Getter;

@Getter
public class UserResponse {

    private final Long id;
    private final String email;

    public UserResponse(Long id, String email) {
        this.id = id;
        this.email = email;
    }

    public static UserResponse from(User user) {
        return new UserResponse(
            user.getId(),
            user.getEmail()
        );
    }
}
