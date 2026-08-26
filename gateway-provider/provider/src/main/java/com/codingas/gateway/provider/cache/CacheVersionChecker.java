/*
 * Copyright © 2025-2026 codingas.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.codingas.gateway.provider.cache;

import com.codingas.gateway.provider.channel.ChannelCredentialRepository;
import com.codingas.gateway.provider.model.ModelRepository;
import com.codingas.gateway.provider.vendor.ProviderRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 缓存版本检查服务（provider 域）
 *
 * <p>定时检查数据库配置版本，作为事件机制的兜底。</p>
 * <p>轮询间隔：30 秒。启动时经 {@link #initVersions()} 记录基线版本，
 * 避免首次轮询把存量数据误判为版本变更。</p>
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class CacheVersionChecker {

    private static final long CHECK_INTERVAL_SECONDS = 30;

    private final ProviderRepository providerRepository;
    private final ModelRepository modelRepository;
    private final ChannelCredentialRepository channelCredentialRepository;
    private final CacheInvalidationManager cacheService;

    // 记录上次检查的版本
    private volatile long lastProviderVersion = 0;
    private volatile long lastModelVersion = 0;
    private volatile long lastCredentialVersion = 0;

    /**
     * 初始化版本号
     *
     * <p>Bean 初始化后自动调用（{@link PostConstruct}），记录当前版本作为轮询基线。</p>
     */
    @PostConstruct
    public void initVersions() {
        lastProviderVersion = providerRepository.getMaxVersion();
        lastModelVersion = modelRepository.getMaxVersion();
        lastCredentialVersion = channelCredentialRepository.getMaxVersion();
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
        long currentProviderVersion = providerRepository.getMaxVersion();
        if (currentProviderVersion > lastProviderVersion) {
            log.info("Provider version changed: {} -> {}", lastProviderVersion, currentProviderVersion);
            cacheService.refreshProviders();
            lastProviderVersion = currentProviderVersion;
        }

        // 检查 Model 版本
        long currentModelVersion = modelRepository.getMaxVersion();
        if (currentModelVersion > lastModelVersion) {
            log.info("Model version changed: {} -> {}", lastModelVersion, currentModelVersion);
            cacheService.refreshModels();
            lastModelVersion = currentModelVersion;
        }

        // 检查 Credential 版本
        long currentCredentialVersion = channelCredentialRepository.getMaxVersion();
        if (currentCredentialVersion > lastCredentialVersion) {
            log.info("Credential version changed: {} -> {}", lastCredentialVersion, currentCredentialVersion);
            cacheService.refreshApiKeys();
            lastCredentialVersion = currentCredentialVersion;
        }
    }
}
