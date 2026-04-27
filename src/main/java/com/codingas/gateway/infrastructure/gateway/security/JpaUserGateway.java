package com.codingas.gateway.infrastructure.gateway.security;

import com.codingas.gateway.domain.security.entity.User;
import com.codingas.gateway.domain.security.gateway.UserGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 用户网关 JPA 实现
 *
 * <p>实现 UserGateway 接口，使用 JPA 进行持久化。</p>
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class JpaUserGateway implements UserGateway {

    private final UserRepository repository;

    @Override
    public Optional<User> findById(Long id) {
        return repository.findById(id);
    }

    @Override
    public User save(User user) {
        return repository.save(user);
    }
}

/**
 * 用户仓储接口
 */
interface UserRepository {
    Optional<User> findById(Long id);
    User save(User user);
}