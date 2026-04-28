package com.codingas.gateway.infrastructure.gateway.router;

import com.codingas.gateway.domain.router.entity.Provider;
import com.codingas.gateway.domain.router.gateway.ProviderGateway;
import com.codingas.gateway.domain.router.repository.ProviderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * 提供商网关 JPA 实现
 *
 * <p>实现 ProviderGateway 接口，使用 JPA 进行持久化。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JpaProviderGateway implements ProviderGateway {

    private final ProviderRepository providerRepository;

    @Override
    public Provider save(Provider provider) {
        return providerRepository.save(provider);
    }

    @Override
    public Optional<Provider> findById(Long id) {
        return providerRepository.findById(id);
    }

    @Override
    public Optional<Provider> findByProviderCode(String providerCode) {
        return providerRepository.findByProviderCode(providerCode);
    }

    @Override
    public List<Provider> findAll() {
        return providerRepository.findAll();
    }

    @Override
    public List<Provider> findAllActive() {
        return providerRepository.findAllActive();
    }

    @Override
    public List<Provider> findByEnabled(Boolean enabled) {
        return providerRepository.findByEnabled(enabled);
    }

    @Override
    public long count() {
        return providerRepository.count();
    }

    @Override
    public void delete(Provider provider) {
        providerRepository.delete(provider);
    }

    @Override
    public boolean existsByProviderCode(String providerCode) {
        return providerRepository.existsByProviderCode(providerCode);
    }
}
