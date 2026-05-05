package com.codingas.gateway.infrastructure.security;

import com.codingas.gateway.domain.security.entity.User;
import com.codingas.gateway.domain.security.gateway.UserGateway;
import com.codingas.gateway.infrastructure.security.database.dataobject.UserDo;
import com.codingas.gateway.infrastructure.security.database.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 用户网关 JPA 实现
 *
 * <p>实现 UserGateway 接口，负责 DO ↔ Entity 转换。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserGatewayImpl implements UserGateway {

    private final UserRepository userRepository;

    @Override
    public User save(User user) {
        UserDo doEntity = toDo(user);
        UserDo saved = userRepository.save(doEntity);
        return toEntity(saved);
    }

    @Override
    public Optional<User> findById(Long id) {
        return userRepository.findById(id).map(this::toEntity);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email).map(this::toEntity);
    }

    @Override
    public List<User> findAll() {
        return userRepository.findAll().stream()
            .map(this::toEntity)
            .collect(Collectors.toList());
    }

    @Override
    public long count() {
        return userRepository.count();
    }

    @Override
    public void delete(User user) {
        userRepository.delete(toDo(user));
    }

    @Override
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Override
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username).map(this::toEntity);
    }

    /**
     * DO 转 Entity
     */
    private User toEntity(UserDo doEntity) {
        if (doEntity == null) {
            return null;
        }
        User user = new User();
        user.setId(doEntity.getId());
        user.setUsername(doEntity.getUsername());
        user.setEmail(doEntity.getEmail());
        user.setPasswordHash(doEntity.getPasswordHash());
        user.setPhone(doEntity.getPhone());
        user.setAvatarUrl(doEntity.getAvatarUrl());
        user.setStatus(doEntity.getStatus());
        user.setEmailVerified(doEntity.getEmailVerified());
        user.setOauthProviders(doEntity.getOauthProviders());
        user.setPiiSalt(doEntity.getPiiSalt());
        user.setLastLoginAt(doEntity.getLastLoginAt());
        user.setDeletedAt(doEntity.getDeletedAt());
        // 角色关联暂不处理，由调用方通过 RoleGateway 获取
        return user;
    }

    /**
     * Entity 转 DO
     */
    private UserDo toDo(User user) {
        if (user == null) {
            return null;
        }
        UserDo doEntity = new UserDo();
        if (user.getId() != null) {
            doEntity.setId(user.getId());
        }
        doEntity.setUsername(user.getUsername());
        doEntity.setEmail(user.getEmail());
        doEntity.setPasswordHash(user.getPasswordHash());
        doEntity.setPhone(user.getPhone());
        doEntity.setAvatarUrl(user.getAvatarUrl());
        doEntity.setStatus(user.getStatus());
        doEntity.setEmailVerified(user.getEmailVerified());
        doEntity.setOauthProviders(user.getOauthProviders());
        doEntity.setPiiSalt(user.getPiiSalt());
        doEntity.setLastLoginAt(user.getLastLoginAt());
        doEntity.setDeletedAt(user.getDeletedAt());
        return doEntity;
    }
}
