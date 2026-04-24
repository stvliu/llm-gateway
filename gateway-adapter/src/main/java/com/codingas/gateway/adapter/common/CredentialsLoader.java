package com.codingas.gateway.adapter.common;

import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 凭证加载器
 *
 * <p>负责从数据库加载加密的 API Key 并在内存中缓存解密后的明文 Key。</p>
 *
 * <p>设计原则:</p>
 * <ul>
 *   <li>启动时加载所有活跃的 Provider API Keys</li>
 *   <li>使用内存缓存避免频繁数据库查询</li>
 *   <li>API Key 解密后存储在内存中</li>
 * </ul>
 *
 * <p>安全说明: 解密后的 API Key 仅存储在内存中，不写日志。</p>
 *
 * <p>当前实现为存根，后续需要和 gateway-core 整合。</p */
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
     *
     * <p>用于管理员修改 Key 后刷新缓存。</p>
     */
    public void reloadAll() {
        log.info("Reloading all provider API keys");
        apiKeyCache.clear();
    }

    /**
     * 初始化加载
     *
     * <p>由 Spring 调用，启动时加载所有活跃的 Provider API Keys。</p>
     */
    public void initialize() {
        log.info("Initializing provider API keys - using stub implementation");
    }

    /**
     * 设置 API Key 到缓存（供测试或初始化使用）
     *
     * @param providerCode 提供商编码
     * @param apiKey API Key
     */
    public void setApiKey(String providerCode, String apiKey) {
        apiKeyCache.put(providerCode, apiKey);
    }
}
