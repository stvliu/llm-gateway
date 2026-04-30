package com.codingas.gateway.domain.supply.service;

import com.codingas.gateway.domain.supply.entity.Provider;
import com.codingas.gateway.domain.supply.gateway.ProviderGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * 提供商服务
 *
 * <p>处理 Provider 的 CRUD 操作和热加载通知。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProviderDomainService {

    private final ProviderGateway providerGateway;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public Optional<Provider> findById(Long id) {
        return providerGateway.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<Provider> findByProviderCode(String providerCode) {
        return providerGateway.findByProviderCode(providerCode);
    }

    @Transactional(readOnly = true)
    public List<Provider> findAll() {
        return providerGateway.findAllActive();
    }

    @Transactional
    public Provider create(Provider provider) {
        if (provider.getStatus() == null) {
            provider.setStatus(Provider.ProviderStatus.ACTIVE);
        }
        Provider saved = providerGateway.save(provider);
        log.info("Created provider: {} ({})", saved.getProviderName(), saved.getProviderCode());
        publishReloadEvent();
        return saved;
    }

    @Transactional
    public Provider update(Long id, Provider provider) {
        Optional<Provider> existingOpt = providerGateway.findById(id);
        if (existingOpt.isEmpty()) {
            throw new IllegalArgumentException("Provider not found: " + id);
        }

        Provider existing = existingOpt.get();
        existing.setProviderName(provider.getProviderName());
        existing.setProviderType(provider.getProviderType());
        existing.setBaseUrl(provider.getBaseUrl());
        existing.setPriority(provider.getPriority());
        existing.setStatus(provider.getStatus());

        Provider updated = providerGateway.save(existing);
        log.info("Updated provider: {} ({})", updated.getProviderName(), updated.getProviderCode());
        publishReloadEvent();
        return updated;
    }

    @Transactional
    public void delete(Long id) {
        Optional<Provider> providerOpt = providerGateway.findById(id);
        if (providerOpt.isEmpty()) {
            throw new IllegalArgumentException("Provider not found: " + id);
        }

        Provider provider = providerOpt.get();
        provider.setStatus(Provider.ProviderStatus.DELETED);
        providerGateway.save(provider);
        log.info("Deleted provider: {}", id);
        publishReloadEvent();
    }

    private void publishReloadEvent() {
        eventPublisher.publishEvent(new ProviderConfigChangedEvent(this));
        log.debug("Published ProviderConfigChangedEvent");
    }

    /**
     * 配置变更事件
     */
    public record ProviderConfigChangedEvent(Object source) {}
}
