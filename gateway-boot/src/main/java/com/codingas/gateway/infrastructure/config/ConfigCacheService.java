package com.codingas.gateway.infrastructure.config;

import com.codingas.gateway.infrastructure.iam.gateway.encryption.EncryptionService;
import com.codingas.gateway.domain.supply.entity.ModelSpec;
import com.codingas.gateway.domain.supply.entity.Provider;
import com.codingas.gateway.domain.supply.gateway.ModelSpecGateway;
import com.codingas.gateway.domain.supply.gateway.ProviderGateway;
import com.codingas.gateway.domain.supply.entity.ChannelCredential;
import com.codingas.gateway.domain.supply.gateway.ChannelCredentialGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * 配置缓存服务
 *
 * <p>使用 Spring Cache 统一缓存抽象，属于技术基础设施。</p>
 * <p>负责 Provider、ModelSpec、ChannelCredential 的缓存管理。</p>
 *
 * <p>注意：已迁移到新架构，使用 ChannelCredential 替代 ProductApiKey。</p>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ConfigCacheService {

    private final ModelSpecGateway modelSpecGateway;
    private final ProviderGateway providerGateway;
    private final ChannelCredentialGateway channelCredentialGateway;
    private final EncryptionService encryptionService;

    // ========== Provider 操作 ==========

    @Cacheable(value = CacheNames.PROVIDERS, key = "#id")
    public Optional<Provider> getProviderById(Long id) {
        return providerGateway.findById(id);
    }

    @Cacheable(value = CacheNames.PROVIDERS, key = "'all'")
    public List<Provider> getAllProviders() {
        return providerGateway.findAll();
    }

    @Cacheable(value = CacheNames.PROVIDERS, key = "'active'")
    public List<Provider> getActiveProviders() {
        return providerGateway.findAllActive();
    }

    // ========== Model 操作 ==========

    @Cacheable(value = CacheNames.MODELS, key = "#id")
    public Optional<ModelSpec> getModelById(Long id) {
        return modelSpecGateway.findById(id);
    }

    @Cacheable(value = CacheNames.MODELS, key = "'all'")
    public List<ModelSpec> getAllModels() {
        return modelSpecGateway.findAll();
    }

    @Cacheable(value = CacheNames.MODELS, key = "'active'")
    public List<ModelSpec> getActiveModels() {
        return modelSpecGateway.findAllActive();
    }

    @Cacheable(value = CacheNames.MODELS, key = "'provider:' + #providerId")
    public List<ModelSpec> getModelsByProviderId(Long providerId) {
        return modelSpecGateway.findByProviderId(providerId);
    }

    // ========== ChannelCredential 操作（敏感数据，仅本地缓存）==========

    /**
     * 获取渠道的默认凭证（解密后）
     *
     * <p>敏感数据，使用本地专用缓存。</p>
     */
    @Cacheable(value = CacheNames.API_KEYS_LOCAL,
               key = "#channelId",
               cacheManager = "localCacheManager")
    public Optional<ChannelCredential> getCredentialByChannelId(Long channelId) {
        return channelCredentialGateway.findDefaultByChannelId(channelId);
    }

    // ========== 缓存刷新 ==========

    @CacheEvict(value = CacheNames.PROVIDERS, allEntries = true)
    public void refreshProviders() {
        log.info("Providers cache refreshed");
    }

    @CacheEvict(value = CacheNames.MODELS, allEntries = true)
    public void refreshModels() {
        log.info("Models cache refreshed");
    }

    @CacheEvict(value = CacheNames.API_KEYS_LOCAL,
                allEntries = true,
                cacheManager = "localCacheManager")
    public void refreshApiKeys() {
        log.info("API Keys cache refreshed");
    }

    public void refreshAll() {
        refreshProviders();
        refreshModels();
        refreshApiKeys();
        log.info("All caches refreshed");
    }
}