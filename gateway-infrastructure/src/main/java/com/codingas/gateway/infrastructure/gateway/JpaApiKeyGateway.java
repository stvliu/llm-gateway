package com.codingas.gateway.core.infrastructure.gateway;

import com.codingas.gateway.core.domain.entity.GatewayApiKey;
import com.codingas.gateway.core.domain.gateway.ApiKeyGateway;
import com.codingas.gateway.core.repository.GatewayApiKeyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * API 密钥网关实现
 *
 * <p>实现 ApiKeyGateway 接口，使用 JPA 进行持久化。</p>
 */
@Component
@RequiredArgsConstructor
public class JpaApiKeyGateway implements ApiKeyGateway {

    private final GatewayApiKeyRepository repository;

    @Override
    public Optional<GatewayApiKey> findByKeyHash(String keyHash) {
        return repository.findByKeyHash(keyHash);
    }

    @Override
    public Optional<GatewayApiKey> findByKeyCode(String keyCode) {
        return repository.findByKeyCode(keyCode);
    }

    @Override
    public List<GatewayApiKey> findByUserId(Long userId) {
        return repository.findByUserId(userId);
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
