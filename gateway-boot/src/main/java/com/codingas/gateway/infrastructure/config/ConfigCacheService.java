/*
 * Copyright (c) 2025 codingas.com
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE file for details.
 */
package com.codingas.gateway.infrastructure.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

/**
 * 配置缓存服务
 *
 * <p>使用 Spring Cache 统一缓存抽象，属于技术基础设施。</p>
 * <p>负责 Provider、Model、ChannelCredential 的缓存刷新：供 {@link ConfigVersionChecker}
 * 在配置版本变更时触发失效。查询缓存已下沉到各 Gateway 实现直接读取，本类仅保留失效入口。</p>
 *
 * <p>注意：已迁移到新架构，使用 ChannelCredential 替代 ProductApiKey。</p>
 */
@Service
@Slf4j
public class ConfigCacheService {

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
}
