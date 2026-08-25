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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cache.annotation.CacheEvict;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ConfigCacheService 测试")
class ConfigCacheServiceTest {

    private final ConfigCacheService service = new ConfigCacheService();

    @Test
    @DisplayName("refreshProviders 声明 providers 缓存全量失效")
    void refreshProviders_cacheEvictMetadata() throws Exception {
        CacheEvict evict = ConfigCacheService.class.getMethod("refreshProviders")
                .getAnnotation(CacheEvict.class);

        assertThat(evict.value()).containsExactly(CacheNames.PROVIDERS);
        assertThat(evict.allEntries()).isTrue();
    }

    @Test
    @DisplayName("refreshModels 声明 models 缓存全量失效")
    void refreshModels_cacheEvictMetadata() throws Exception {
        CacheEvict evict = ConfigCacheService.class.getMethod("refreshModels")
                .getAnnotation(CacheEvict.class);

        assertThat(evict.value()).containsExactly(CacheNames.MODELS);
        assertThat(evict.allEntries()).isTrue();
    }

    @Test
    @DisplayName("refreshApiKeys 声明本地 apiKeysLocal 缓存全量失效")
    void refreshApiKeys_cacheEvictMetadata() throws Exception {
        CacheEvict evict = ConfigCacheService.class.getMethod("refreshApiKeys")
                .getAnnotation(CacheEvict.class);

        assertThat(evict.value()).containsExactly(CacheNames.API_KEYS_LOCAL);
        assertThat(evict.allEntries()).isTrue();
        assertThat(evict.cacheManager()).isEqualTo("localCacheManager");
    }
}
