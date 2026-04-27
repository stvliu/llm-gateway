package com.codingas.gateway.infrastructure.gateway.security;

import com.codingas.gateway.domain.security.entity.GatewayApiKey;
import com.codingas.gateway.domain.security.gateway.ApiKeyGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * API Key 网关 JPA 实现
 *
 * <p>实现 ApiKeyGateway 接口，使用 JPA 进行持久化。</p>
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class JpaApiKeyGateway implements ApiKeyGateway {

    private final GatewayApiKeyRepository repository;

    @Override
    public GatewayApiKey findByKeyHash(String keyHash) {
        return repository.findByKeyHash(keyHash).orElse(null);
    }

    @Override
    public GatewayApiKey findByKeyCode(String keyCode) {
        return repository.findByKeyCode(keyCode).orElse(null);
    }

    @Override
    public List<GatewayApiKey> findByUserId(Long userId) {
        return repository.findByUserId(userId);
    }

    @Override
    public Page<GatewayApiKey> findExpiringKeys(Instant now, Instant threshold, Pageable pageable) {
        return repository.findExpiringKeys(now, threshold, pageable);
    }

    @Override
    public GatewayApiKey save(GatewayApiKey apiKey) {
        return repository.save(apiKey);
    }

    @Override
    public void updateLastUsed(String keyCode, Instant lastUsed) {
        repository.findByKeyCode(keyCode).ifPresent(key -> {
            key.setLastUsedAt(lastUsed);
            repository.save(key);
        });
    }
}

/**
 * API Key 仓储接口
 */
interface GatewayApiKeyRepository {
    Optional<GatewayApiKey> findByKeyHash(String keyHash);
    Optional<GatewayApiKey> findByKeyCode(String keyCode);
    List<GatewayApiKey> findByUserId(Long userId);
    Page<GatewayApiKey> findExpiringKeys(Instant now, Instant threshold, Pageable pageable);
    GatewayApiKey save(GatewayApiKey apiKey);
}