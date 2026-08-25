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

/**
 * 缓存名称常量（provider 域）
 *
 * <p>定义 provider 域数据的缓存名称。</p>
 */
public final class CacheNames {

    private CacheNames() {
        // 私有构造函数，防止实例化
    }

    /**
     * Provider 缓存（可共享到 Redis）
     */
    public static final String PROVIDERS = "providers";

    /**
     * Model 缓存（可共享到 Redis）
     */
    public static final String MODELS = "models";

    /**
     * API Key 缓存（敏感数据，仅本地）
     */
    public static final String API_KEYS_LOCAL = "apiKeysLocal";
}
