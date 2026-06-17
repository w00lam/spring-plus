package org.example.expert.domain.common.dto;

import java.security.Principal;

import org.example.expert.domain.user.entity.User;

import lombok.Getter;

@Getter
public class AuthenticatedUser implements Principal {
	private final User user;
	private final String name;

	public AuthenticatedUser(User user) {
		this.user = user;
		this.name = user.getNickname();
	}

	public static User fromPrincipal(Principal principal) {
		return ((AuthenticatedUser) principal).getUser();
	}
}
