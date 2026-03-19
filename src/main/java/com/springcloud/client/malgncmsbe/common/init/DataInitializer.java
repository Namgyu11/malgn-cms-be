package com.springcloud.client.malgncmsbe.common.init;

import com.springcloud.client.malgncmsbe.user.domain.Role;
import com.springcloud.client.malgncmsbe.user.domain.User;
import com.springcloud.client.malgncmsbe.user.domain.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        userRepository.save(User.builder()
                .username("admin")
                .password(passwordEncoder.encode("admin1234"))
                .role(Role.ADMIN)
                .build());

        userRepository.save(User.builder()
                .username("user1")
                .password(passwordEncoder.encode("user11234"))
                .role(Role.USER)
                .build());

        userRepository.save(User.builder()
                .username("user2")
                .password(passwordEncoder.encode("user21234"))
                .role(Role.USER)
                .build());
    }
}
