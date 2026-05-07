package com.codingas.gateway.infrastructure.security;

import com.codingas.gateway.domain.security.entity.GatewayApiKey;
import com.codingas.gateway.domain.security.gateway.ApiKeyGateway;
import com.codingas.gateway.infrastructure.security.database.dataobject.GatewayApiKeyDo;
import com.codingas.gateway.infrastructure.security.database.GatewayApiKeyRepository;
import com.codingas.gateway.infrastructure.security.database.dataobject.UserDo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * API Key 网关 JPA 实现
 *
 * <p>实现 ApiKeyGateway 接口，负责 DO ↔ Entity 转换。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApiKeyGatewayImpl implements ApiKeyGateway {

    private final GatewayApiKeyRepository gatewayApiKeyRepository;

    @Override
    public GatewayApiKey save(GatewayApiKey apiKey) {
        GatewayApiKeyDo doEntity = toDo(apiKey);
        GatewayApiKeyDo saved = gatewayApiKeyRepository.save(doEntity);
        return toEntity(saved);
    }

    @Override
    public Optional<GatewayApiKey> findById(Long id) {
        return gatewayApiKeyRepository.findById(id).map(this::toEntity);
    }

    @Override
    public GatewayApiKey findByKeyHash(String keyHash) {
        return gatewayApiKeyRepository.findByKeyHash(keyHash).map(this::toEntity).orElse(null);
    }

    @Override
    public List<GatewayApiKey> findByUserId(Long userId) {
        return gatewayApiKeyRepository.findByUserId(userId).stream()
            .map(this::toEntity)
            .collect(Collectors.toList());
    }

    @Override
    public List<GatewayApiKey> findAll() {
        return gatewayApiKeyRepository.findAll().stream()
            .map(this::toEntity)
            .collect(Collectors.toList());
    }

    @Override
    public Page<GatewayApiKey> findExpiringKeys(Instant now, Instant threshold, Pageable pageable) {
        return gatewayApiKeyRepository.findExpiringKeys(now, threshold, pageable)
            .map(this::toEntity);
    }

    @Override
    public long count() {
        return gatewayApiKeyRepository.count();
    }

    @Override
    public void delete(GatewayApiKey apiKey) {
        gatewayApiKeyRepository.delete(toDo(apiKey));
    }

    @Override
    public void updateLastUsed(Long id, Instant lastUsed) {
        gatewayApiKeyRepository.findById(id).ifPresent(key -> {
            key.setLastUsedAt(lastUsed);
            gatewayApiKeyRepository.save(key);
        });
    }

    /**
     * DO 转 Entity
     */
    private GatewayApiKey toEntity(GatewayApiKeyDo doEntity) {
        if (doEntity == null) {
            return null;
        }
        GatewayApiKey entity = new GatewayApiKey();
        entity.setId(doEntity.getId());
        entity.setKeyHash(doEntity.getKeyHash());
        entity.setName(doEntity.getName());
        entity.setExpiresAt(doEntity.getExpiresAt());
        entity.setLastUsedAt(doEntity.getLastUsedAt());
        entity.setIpWhitelist(doEntity.getIpWhitelist());
        entity.setDeletedAt(doEntity.getDeletedAt());
        entity.setCreatedAt(doEntity.getCreatedAt());
        entity.setUpdatedAt(doEntity.getUpdatedAt());
        // 枚举转换
        if (doEntity.getStatus() != null) {
            entity.setStatus(GatewayApiKey.ApiKeyStatus.valueOf(doEntity.getStatus().name()));
        }
        // User 关联 - 使用 ID 引用
        if (doEntity.getUser() != null) {
            entity.setUserId(doEntity.getUser().getId());
            entity.setUsername(doEntity.getUser().getUsername());
        }
        return entity;
    }

    /**
     * Entity 转 DO
     */
    private GatewayApiKeyDo toDo(GatewayApiKey entity) {
        if (entity == null) {
            return null;
        }
        GatewayApiKeyDo doEntity = new GatewayApiKeyDo();
        if (entity.getId() != null) {
            doEntity.setId(entity.getId());
        }
        doEntity.setKeyHash(entity.getKeyHash());
        doEntity.setName(entity.getName());
        doEntity.setExpiresAt(entity.getExpiresAt());
        doEntity.setLastUsedAt(entity.getLastUsedAt());
        doEntity.setIpWhitelist(entity.getIpWhitelist());
        doEntity.setDeletedAt(entity.getDeletedAt());
        // 枚举转换
        if (entity.getStatus() != null) {
            doEntity.setStatus(GatewayApiKeyDo.ApiKeyStatus.valueOf(entity.getStatus().name()));
        }
        // User 关联 - 只需要设置 ID
        if (entity.getUserId() != null) {
            UserDo userDo = new UserDo();
            userDo.setId(entity.getUserId());
            doEntity.setUser(userDo);
        }
        return doEntity;
    }
}
