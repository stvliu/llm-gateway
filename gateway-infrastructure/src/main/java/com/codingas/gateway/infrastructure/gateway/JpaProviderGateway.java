package com.codingas.gateway.core.infrastructure.gateway;

import com.codingas.gateway.core.domain.entity.Provider;
import com.codingas.gateway.core.domain.gateway.ProviderGateway;
import com.codingas.gateway.core.domain.enums.ProviderStatus;
import com.codingas.gateway.core.repository.ProviderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * 提供商网关实现
 *
 * <p>实现 ProviderGateway 接口，使用 JPA 进行持久化。</p>
 */
@Component
@RequiredArgsConstructor
public class JpaProviderGateway implements ProviderGateway {

    private final ProviderRepository repository;

    @Override
    public Optional<Provider> findByProviderCode(String providerCode) {
        return repository.findByProviderCode(providerCode);
    }

    @Override
    public Optional<Provider> findById(Long providerId) {
        return repository.findById(providerId);
    }

    @Override
    public List<Provider> findByStatus(ProviderStatus status) {
        return repository.findByStatus(status);
    }

    @Override
    public List<Provider> findAllActive() {
        return repository.findByStatus(ProviderStatus.ACTIVE);
    }

    @Override
    public Provider save(Provider provider) {
        return repository.save(provider);
    }
}
