package com.codingas.gateway.infrastructure.gateway.router;

import com.codingas.gateway.domain.router.entity.Provider;
import com.codingas.gateway.domain.router.gateway.ProviderGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 提供商网关 JPA 实现
 *
 * <p>实现 ProviderGateway 接口，使用 JPA 进行持久化。</p>
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class JpaProviderGateway implements ProviderGateway {

    private final ProviderRepository repository;

    @Override
    public Provider findById(Long id) {
        return repository.findById(id).orElse(null);
    }

    @Override
    public Provider findByProviderCode(String providerCode) {
        return repository.findByProviderCode(providerCode).orElse(null);
    }

    @Override
    public List<Provider> findAllActive() {
        return repository.findAllActive();
    }

    @Override
    public List<Provider> findByEnabled(Boolean enabled) {
        return repository.findByEnabled(enabled);
    }

    @Override
    public Provider save(Provider provider) {
        return repository.save(provider);
    }
}

/**
 * 提供商仓储接口
 */
interface ProviderRepository {
    java.util.Optional<Provider> findById(Long id);
    java.util.Optional<Provider> findByProviderCode(String providerCode);
    List<Provider> findAllActive();
    List<Provider> findByEnabled(Boolean enabled);
    Provider save(Provider provider);
}