package com.codingas.gateway.infrastructure.util;

import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 凭证加载器
 *
 * <p>负责从数据库加载加密的 API Key 并在内存中缓存解密后的明文 Key。</p>
 */
@Slf4j
public class CredentialsLoader {

    /** 内存缓存: providerCode -> decrypted API Key */
    private final Map<String, String> apiKeyCache = new ConcurrentHashMap<>();

    /**
     * 获取指定提供商的 API Key
     *
     * @param providerCode 提供商编码 (如 "openai", "anthropic")
     * @return 解密后的 API Key
     */
    public Optional<String> getApiKey(String providerCode) {
        return Optional.ofNullable(apiKeyCache.get(providerCode));
    }

    /**
     * 重新加载所有凭证
     */
    public void reloadAll() {
        log.info("Reloading all provider API keys");
        apiKeyCache.clear();
    }

    /**
     * 初始化加载
     */
    public void initialize() {
        log.info("Initializing provider API keys - using stub implementation");
    }

    /**
     * 设置 API Key 到缓存
     *
     * @param providerCode 提供商编码
     * @param apiKey API Key
     */
    public void setApiKey(String providerCode, String apiKey) {
        apiKeyCache.put(providerCode, apiKey);
    }
}
