package org.example.expert.domain.user.service;

import org.example.expert.config.PasswordEncoder;
import org.example.expert.domain.common.exception.InvalidRequestException;
import org.example.expert.domain.user.dto.response.UserSearchResponse;
import org.example.expert.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Test
    void searchesUsersByExactNickname() {
        String nickname = "exactNickname";
        List<UserSearchResponse> expected = List.of(
                new UserSearchResponse(1L, "user@example.com", nickname)
        );
        when(userRepository.findAllByNickname(nickname)).thenReturn(expected);

        List<UserSearchResponse> result = userService.searchUsersByNickname(nickname);

        assertThat(result).isEqualTo(expected);
        verify(userRepository).findAllByNickname(nickname);
    }

    @Test
    void rejectsBlankNickname() {
        assertThatThrownBy(() -> userService.searchUsersByNickname(" "))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("Nickname must not be blank");
    }
}
