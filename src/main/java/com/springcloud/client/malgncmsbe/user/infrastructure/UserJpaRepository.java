package com.springcloud.client.malgncmsbe.user.infrastructure;

import com.springcloud.client.malgncmsbe.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

interface UserJpaRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);
}
