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
package com.codingas.gateway.infrastructure.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import java.time.Duration;

/**
 * 缓存配置
 *
 * <p>提供两个独立的 CacheManager：</p>
 * <ul>
 *   <li>localCacheManager - 本地缓存（Caffeine），用于敏感数据</li>
 *   <li>distributedCacheManager - 分布式缓存（Redis），企业版启用</li>
 * </ul>
 */
@Configuration
@EnableCaching
@Slf4j
public class CacheConfig {

    // ========== 本地缓存管理器（始终存在）==========

    /**
     * 本地缓存管理器
     *
     * <p>用于敏感数据（API Key），始终使用 Caffeine 本地缓存。</p>
     * <p>标准版和企业版都使用此缓存管理器存储敏感数据。</p>
     */
    @Bean("localCacheManager")
    public CacheManager localCacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager();
        manager.setCaffeine(Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(Duration.ofHours(1))
            .recordStats());
        log.info("Local cache manager (Caffeine) initialized");
        return manager;
    }

    // ========== 标准版：默认使用本地缓存 ==========

    /**
     * 标准版默认缓存管理器
     *
     * <p>单实例部署，所有数据使用本地缓存。</p>
     */
    @Bean
    @Primary
    @Profile({"local", "dev", "standalone"})
    public CacheManager defaultCacheManagerStandalone(
            @Qualifier("localCacheManager") CacheManager localCacheManager) {
        log.info("Using local cache as default (standalone mode)");
        return localCacheManager;
    }
}
