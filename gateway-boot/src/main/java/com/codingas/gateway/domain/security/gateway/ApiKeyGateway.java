package com.codingas.gateway.domain.security.gateway;

import com.codingas.gateway.domain.security.entity.GatewayApiKey;

/**
 * API Key 网关接口
 *
 * <p>定义在 domain 层，由 infrastructure 层实现。</p>
 * <p>Domain 不直接依赖持久化，通过此接口操作。</p>
 */
public interface ApiKeyGateway {

    /**
     * 根据 Key Hash 查找 API Key
     *
     * @param keyHash 密钥哈希值
     * @return 密钥信息，不存在返回 null
     */
    GatewayApiKey findByKeyHash(String keyHash);

    /**
     * 根据 Key Code 查找 API Key
     *
     * @param keyCode 密钥代码
     * @return 密钥信息，不存在返回 null
     */
    GatewayApiKey findByKeyCode(String keyCode);

    /**
     * 保存 API Key
     *
     * @param apiKey 密钥实体
     * @return 保存后的实体
     */
    GatewayApiKey save(GatewayApiKey apiKey);

    /**
     * 更新最后使用时间
     *
     * @param keyCode 密钥代码
     * @param lastUsed 最后使用时间
     */
    void updateLastUsed(String keyCode, java.time.Instant lastUsed);
}