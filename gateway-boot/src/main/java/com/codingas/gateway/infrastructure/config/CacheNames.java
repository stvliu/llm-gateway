package com.codingas.gateway.infrastructure.config;

/**
 * 缓存名称常量
 *
 * <p>定义系统中所有缓存的名称。</p>
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
