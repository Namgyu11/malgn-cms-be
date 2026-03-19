package com.springcloud.client.malgncmsbe.common.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderTest {

    private static final String TEST_SECRET = "malgn-cms-secret-key-must-be-at-least-256-bits-long-for-hs256-algorithm";
    private static final long ONE_DAY_MS = 86_400_000L;

    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider(TEST_SECRET, ONE_DAY_MS);
    }

    @Test
    @DisplayName("토큰 생성 후 username과 role을 정상적으로 추출한다")
    void generateToken_shouldContainUsernameAndRole() {
        String token = jwtTokenProvider.generateToken("user1", "USER");

        assertThat(jwtTokenProvider.getUsername(token)).isEqualTo("user1");
        assertThat(jwtTokenProvider.getRole(token)).isEqualTo("USER");
    }

    @Test
    @DisplayName("유효한 토큰 검증은 true를 반환한다")
    void validateToken_shouldReturnTrue_whenTokenIsValid() {
        String token = jwtTokenProvider.generateToken("user1", "USER");

        assertThat(jwtTokenProvider.validateToken(token)).isTrue();
    }

    @Test
    @DisplayName("잘못된 토큰 검증은 false를 반환한다")
    void validateToken_shouldReturnFalse_whenTokenIsInvalid() {
        assertThat(jwtTokenProvider.validateToken("invalid.token.value")).isFalse();
    }

    @Test
    @DisplayName("빈 문자열 토큰 검증은 false를 반환한다")
    void validateToken_shouldReturnFalse_whenTokenIsEmpty() {
        assertThat(jwtTokenProvider.validateToken("")).isFalse();
    }
}
