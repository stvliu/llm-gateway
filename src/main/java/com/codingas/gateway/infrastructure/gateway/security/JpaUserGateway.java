package com.codingas.gateway.infrastructure.gateway.security;

import com.codingas.gateway.domain.security.entity.User;
import com.codingas.gateway.domain.security.gateway.UserGateway;
import com.codingas.gateway.domain.security.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * 用户网关 JPA 实现
 *
 * <p>实现 UserGateway 接口，使用 JPA 进行持久化。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JpaUserGateway implements UserGateway {

    private final UserRepository userRepository;

    @Override
    public User save(User user) {
        return userRepository.save(user);
    }

    @Override
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    @Override
    public Optional<User> findByUserCode(String userCode) {
        return userRepository.findByUserCode(userCode);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Override
    public List<User> findAll() {
        return userRepository.findAll();
    }

    @Override
    public long count() {
        return userRepository.count();
    }

    @Override
    public void delete(User user) {
        userRepository.delete(user);
    }

    @Override
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Override
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }
}
