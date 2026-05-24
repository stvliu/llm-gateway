package com.codingas.gateway.infrastructure.config;

import com.codingas.gateway.domain.supply.gateway.ModelSpecGateway;
import com.codingas.gateway.domain.supply.gateway.ProviderGateway;
import com.codingas.gateway.domain.supply.gateway.ChannelCredentialGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 配置版本检查服务
 *
 * <p>定时检查数据库配置版本，作为事件机制的兜底。</p>
 * <p>轮询间隔：30 秒</p>
 *
 * <p>注意：已迁移到新架构，使用 ChannelCredentialGateway 替代 ProductApiKeyGateway。</p>
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ConfigVersionChecker {

    private static final long CHECK_INTERVAL_SECONDS = 30;

    private final ProviderGateway providerGateway;
    private final ModelSpecGateway modelSpecGateway;
    private final ChannelCredentialGateway channelCredentialGateway;
    private final ConfigCacheService cacheService;

    // 记录上次检查的版本
    private volatile long lastProviderVersion = 0;
    private volatile long lastModelVersion = 0;
    private volatile long lastCredentialVersion = 0;

    /**
     * 初始化版本号
     *
     * <p>在应用启动时调用，记录当前版本。</p>
     */
    public void initVersions() {
        lastProviderVersion = providerGateway.getMaxVersion();
        lastModelVersion = modelSpecGateway.getMaxVersion();
        lastCredentialVersion = channelCredentialGateway.getMaxVersion();
        log.info("Version checker initialized: provider={}, model={}, credential={}",
            lastProviderVersion, lastModelVersion, lastCredentialVersion);
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
        long currentModelVersion = modelSpecGateway.getMaxVersion();
        if (currentModelVersion > lastModelVersion) {
            log.info("Model version changed: {} -> {}", lastModelVersion, currentModelVersion);
            cacheService.refreshModels();
            lastModelVersion = currentModelVersion;
        }

        // 检查 Credential 版本
        long currentCredentialVersion = channelCredentialGateway.getMaxVersion();
        if (currentCredentialVersion > lastCredentialVersion) {
            log.info("Credential version changed: {} -> {}", lastCredentialVersion, currentCredentialVersion);
            cacheService.refreshApiKeys();
            lastCredentialVersion = currentCredentialVersion;
        }
    }
}