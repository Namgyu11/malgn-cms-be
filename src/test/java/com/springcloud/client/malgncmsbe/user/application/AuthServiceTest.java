package com.springcloud.client.malgncmsbe.user.application;

import com.springcloud.client.malgncmsbe.common.exception.BusinessException;
import com.springcloud.client.malgncmsbe.common.exception.ErrorCode;
import com.springcloud.client.malgncmsbe.common.security.JwtTokenProvider;
import com.springcloud.client.malgncmsbe.user.application.command.LoginCommand;
import com.springcloud.client.malgncmsbe.user.application.command.SignupCommand;
import com.springcloud.client.malgncmsbe.user.application.result.TokenResult;
import com.springcloud.client.malgncmsbe.user.domain.Role;
import com.springcloud.client.malgncmsbe.user.domain.User;
import com.springcloud.client.malgncmsbe.user.domain.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @InjectMocks
    private AuthService authService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Test
    @DisplayName("올바른 자격증명으로 로그인하면 토큰을 반환한다")
    void login_shouldReturnToken_whenCredentialsAreValid() {
        User user = User.builder().username("user1").password("encoded").role(Role.USER).build();
        given(userRepository.findByUsername("user1")).willReturn(Optional.of(user));
        given(passwordEncoder.matches("password", "encoded")).willReturn(true);
        given(jwtTokenProvider.generateToken("user1", "USER")).willReturn("jwt-token");

        TokenResult result = authService.login(new LoginCommand("user1", "password"));

        assertThat(result.accessToken()).isEqualTo("jwt-token");
    }

    @Test
    @DisplayName("존재하지 않는 사용자로 로그인하면 INVALID_CREDENTIALS 예외를 던진다")
    void login_shouldThrowException_whenUserNotFound() {
        given(userRepository.findByUsername("unknown")).willReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginCommand("unknown", "password")))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_CREDENTIALS);
    }

    @Test
    @DisplayName("비밀번호 불일치 시 INVALID_CREDENTIALS 예외를 던진다")
    void login_shouldThrowException_whenPasswordMismatch() {
        User user = User.builder().username("user1").password("encoded").role(Role.USER).build();
        given(userRepository.findByUsername("user1")).willReturn(Optional.of(user));
        given(passwordEncoder.matches("wrong", "encoded")).willReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginCommand("user1", "wrong")))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_CREDENTIALS);
    }

    @Test
    @DisplayName("신규 username으로 회원가입하면 사용자가 저장된다")
    void signup_shouldSaveUser_whenUsernameIsAvailable() {
        given(userRepository.existsByUsername("newuser")).willReturn(false);
        given(passwordEncoder.encode("password1")).willReturn("encoded");
        given(userRepository.save(any(User.class))).willAnswer(inv -> inv.getArgument(0));

        authService.signup(new SignupCommand("newuser", "password1"));

        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("중복 username으로 회원가입하면 DUPLICATE_USERNAME 예외를 던진다")
    void signup_shouldThrowException_whenUsernameIsDuplicate() {
        given(userRepository.existsByUsername("user1")).willReturn(true);

        assertThatThrownBy(() -> authService.signup(new SignupCommand("user1", "password1")))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DUPLICATE_USERNAME);
    }
}
