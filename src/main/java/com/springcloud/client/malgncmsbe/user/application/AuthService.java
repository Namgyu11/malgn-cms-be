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
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public TokenResult login(LoginCommand command) {
        User user = userRepository.findByUsername(command.username())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS));

        if (!passwordEncoder.matches(command.password(), user.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }

        String token = jwtTokenProvider.generateToken(user.getUsername(), user.getRole().name());
        return new TokenResult(token);
    }

    @Transactional
    public void signup(SignupCommand command) {
        if (userRepository.existsByUsername(command.username())) {
            throw new BusinessException(ErrorCode.DUPLICATE_USERNAME);
        }

        User user = User.builder()
                .username(command.username())
                .password(passwordEncoder.encode(command.password()))
                .role(Role.USER)
                .build();

        userRepository.save(user);
    }
}
