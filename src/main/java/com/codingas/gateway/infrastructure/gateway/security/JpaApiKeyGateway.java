package com.codingas.gateway.infrastructure.gateway.security;

import com.codingas.gateway.domain.security.entity.GatewayApiKey;
import com.codingas.gateway.domain.security.gateway.ApiKeyGateway;
import com.codingas.gateway.domain.security.repository.GatewayApiKeyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * API Key 网关 JPA 实现
 *
 * <p>实现 ApiKeyGateway 接口，使用 JPA 进行持久化。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JpaApiKeyGateway implements ApiKeyGateway {

    private final GatewayApiKeyRepository gatewayApiKeyRepository;

    @Override
    public GatewayApiKey save(GatewayApiKey apiKey) {
        return gatewayApiKeyRepository.save(apiKey);
    }

    @Override
    public Optional<GatewayApiKey> findById(Long id) {
        return gatewayApiKeyRepository.findById(id);
    }

    @Override
    public GatewayApiKey findByKeyHash(String keyHash) {
        return gatewayApiKeyRepository.findByKeyHash(keyHash).orElse(null);
    }

    @Override
    public GatewayApiKey findByKeyCode(String keyCode) {
        return gatewayApiKeyRepository.findByKeyCode(keyCode).orElse(null);
    }

    @Override
    public List<GatewayApiKey> findByUserId(Long userId) {
        return gatewayApiKeyRepository.findByUserId(userId);
    }

    @Override
    public List<GatewayApiKey> findAll() {
        return gatewayApiKeyRepository.findAll();
    }

    @Override
    public Page<GatewayApiKey> findExpiringKeys(Instant now, Instant threshold, Pageable pageable) {
        return gatewayApiKeyRepository.findExpiringKeys(now, threshold, pageable);
    }

    @Override
    public long count() {
        return gatewayApiKeyRepository.count();
    }

    @Override
    public void delete(GatewayApiKey apiKey) {
        gatewayApiKeyRepository.delete(apiKey);
    }

    @Override
    public boolean existsByKeyCode(String keyCode) {
        return gatewayApiKeyRepository.existsByKeyCode(keyCode);
    }

    @Override
    public void updateLastUsed(String keyCode, Instant lastUsed) {
        gatewayApiKeyRepository.findByKeyCode(keyCode).ifPresent(key -> {
            key.setLastUsedAt(lastUsed);
            gatewayApiKeyRepository.save(key);
        });
    }
}
