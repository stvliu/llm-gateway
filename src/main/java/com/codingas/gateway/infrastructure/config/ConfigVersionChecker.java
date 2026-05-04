package com.codingas.gateway.infrastructure.config;

import com.codingas.gateway.domain.model.gateway.ModelGateway;
import com.codingas.gateway.domain.model.gateway.ProviderApiKeyGateway;
import com.codingas.gateway.domain.model.gateway.ProviderGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 配置版本检查服务
 *
 * <p>定时检查数据库配置版本，作为事件机制的兜底。</p>
 * <p>轮询间隔：30 秒</p>
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ConfigVersionChecker {

    private static final long CHECK_INTERVAL_SECONDS = 30;

    private final ProviderGateway providerGateway;
    private final ModelGateway modelGateway;
    private final ProviderApiKeyGateway apiKeyGateway;
    private final ConfigCacheService cacheService;

    // 记录上次检查的版本
    private volatile long lastProviderVersion = 0;
    private volatile long lastModelVersion = 0;
    private volatile long lastApiKeyVersion = 0;

    /**
     * 初始化版本号
     *
     * <p>在应用启动时调用，记录当前版本。</p>
     */
    public void initVersions() {
        lastProviderVersion = providerGateway.getMaxVersion();
        lastModelVersion = modelGateway.getMaxVersion();
        lastApiKeyVersion = apiKeyGateway.getMaxVersion();
        log.info("Version checker initialized: provider={}, model={}, apiKey={}",
            lastProviderVersion, lastModelVersion, lastApiKeyVersion);
    }

    /**
     * 检查版本变化
     *
     * <p>定时执行，检测版本变化并刷新缓存。</p>
     */
    @Scheduled(fixedRate = CHECK_INTERVAL_SECONDS * 1000)
    public void checkVersions() {
        // 检查 Provider 版本
        long currentProviderVersion = providerGateway.getMaxVersion();
        if (currentProviderVersion > lastProviderVersion) {
            log.info("Provider version changed: {} -> {}", lastProviderVersion, currentProviderVersion);
            cacheService.refreshProviders();
            lastProviderVersion = currentProviderVersion;
        }

        // 检查 Model 版本
        long currentModelVersion = modelGateway.getMaxVersion();
        if (currentModelVersion > lastModelVersion) {
            log.info("Model version changed: {} -> {}", lastModelVersion, currentModelVersion);
            cacheService.refreshModels();
            lastModelVersion = currentModelVersion;
        }

        // 检查 API Key 版本
        long currentApiKeyVersion = apiKeyGateway.getMaxVersion();
        if (currentApiKeyVersion > lastApiKeyVersion) {
            log.info("API Key version changed: {} -> {}", lastApiKeyVersion, currentApiKeyVersion);
            cacheService.refreshApiKeys();
            lastApiKeyVersion = currentApiKeyVersion;
        }
    }
}
