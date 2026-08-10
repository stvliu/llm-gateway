/*
 * Copyright (c) 2025 codingas.com
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE file for details.
 */
package com.codingas.gateway.infrastructure.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

class CacheConfigTest {

    @Test
    @DisplayName("should create localCacheManager bean")
    void shouldCreateLocalCacheManagerBean() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.register(CacheConfig.class);
            context.register(TestConfig.class);
            context.refresh();

            CacheManager cacheManager = context.getBean("localCacheManager", CacheManager.class);
            assertThat(cacheManager).isNotNull();
        }
    }

    @EnableCaching
    static class TestConfig {
        // 测试配置
    }
}
