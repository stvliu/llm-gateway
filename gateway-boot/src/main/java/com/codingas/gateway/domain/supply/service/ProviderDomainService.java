package com.codingas.gateway.domain.supply.service;

import com.codingas.gateway.domain.supply.entity.Provider;
import com.codingas.gateway.domain.supply.enums.ProviderState;
import com.codingas.gateway.domain.supply.exception.ProviderException;
import com.codingas.gateway.domain.supply.gateway.ProviderGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * 供应商领域服务
 *
 * <p>封装供应商相关的核心业务逻辑。</p>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ProviderDomainService {

    private final ProviderGateway providerGateway;

    /**
     * 创建供应商
     *
     * @throws ProviderException 供应商 code 或 name 已存在
     */
    public Provider create(Provider provider) {
        if (providerGateway.existsByCode(provider.getCode())) {
            throw new ProviderException("PROVIDER_CODE_DUPLICATE", "供应商代码已存在: " + provider.getCode());
        }
        if (providerGateway.existsByName(provider.getName())) {
            throw new ProviderException("PROVIDER_NAME_DUPLICATE", "供应商名称已存在: " + provider.getName());
        }
        return providerGateway.save(provider);
    }

    /**
     * 更新供应商
     */
    public Provider update(Provider provider) {
        return providerGateway.save(provider);
    }

    /**
     * 启用供应商
     */
    public Provider enable(Long id) {
        Provider provider = providerGateway.findById(id)
                .orElseThrow(() -> new ProviderException("PROVIDER_NOT_FOUND", "供应商不存在: " + id));
        provider.setState(ProviderState.ACTIVE);
        return providerGateway.save(provider);
    }

    /**
     * 禁用供应商
     */
    public Provider disable(Long id) {
        Provider provider = providerGateway.findById(id)
                .orElseThrow(() -> new ProviderException("PROVIDER_NOT_FOUND", "供应商不存在: " + id));
        provider.setState(ProviderState.DISABLED);
        return providerGateway.save(provider);
    }

    /**
     * 软删除供应商
     */
    public void delete(Long id) {
        Provider provider = providerGateway.findById(id)
                .orElseThrow(() -> new ProviderException("PROVIDER_NOT_FOUND", "供应商不存在: " + id));
        provider.setState(ProviderState.DELETED);
        providerGateway.save(provider);
    }

    /**
     * 根据代码查找供应商
     */
    public Optional<Provider> findByCode(String code) {
        return providerGateway.findByCode(code);
    }

    /**
     * 查找所有活跃供应商
     */
    public List<Provider> findAllActive() {
        return providerGateway.findAllActive();
    }

    /**
     * 查找所有供应商
     */
    public List<Provider> findAll() {
        return providerGateway.findAll();
    }
}