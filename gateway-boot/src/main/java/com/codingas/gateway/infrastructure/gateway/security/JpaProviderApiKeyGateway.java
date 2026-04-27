package com.codingas.gateway.infrastructure.gateway.security;

import com.codingas.gateway.domain.security.entity.ProviderApiKey;
import com.codingas.gateway.domain.security.gateway.ProviderApiKeyGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * 提供商 API 密钥网关实现
 *
 * <p>实现 ProviderApiKeyGateway 接口，使用 JPA 进行持久化。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JpaProviderApiKeyGateway implements ProviderApiKeyGateway {

    private final ProviderApiKeyRepository repository;

    @Override
    public Optional<ProviderApiKey> findByProviderCode(String providerCode) {
        return repository.findByKeyCode(providerCode);
    }

    @Override
    public Optional<ProviderApiKey> findById(Long id) {
        return repository.findById(id);
    }

    @Override
    public ProviderApiKey save(ProviderApiKey providerApiKey) {
        return repository.save(providerApiKey);
    }
}