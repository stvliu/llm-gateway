package com.codingas.gateway.core.domain.gateway;

import com.codingas.gateway.core.domain.entity.GatewayApiKey;

import java.time.Instant;
import java.util.Optional;

/**
 * API 密钥网关接口
 *
 * <p>定义在 domain 层，由 infrastructure 层实现。</p>
 * <p>Domain 不直接依赖持久化，通过此接口操作 API 密钥。</p>
 */
public interface ApiKeyGateway {

    /**
     * 根据 API Key 哈希查找密钥信息
     *
     * @param keyHash API Key 哈希值
     * @return 密钥信息，不存在返回空
     */
    Optional<GatewayApiKey> findByKeyHash(String keyHash);

    /**
     * 根据密钥编码查找密钥信息
     *
     * @param keyCode 密钥编码
     * @return 密钥信息，不存在返回空
     */
    Optional<GatewayApiKey> findByKeyCode(String keyCode);

    /**
     * 根据用户 ID 查找所有密钥
     *
     * @param userId 用户 ID
     * @return 密钥列表
     */
    java.util.List<GatewayApiKey> findByUserId(Long userId);

    /**
     * 保存 API 密钥
     *
     * @param apiKey 密钥实体
     * @return 保存后的实体
     */
    GatewayApiKey save(GatewayApiKey apiKey);

    /**
     * 更新最后使用时间
     *
     * @param keyCode 密钥编码
     * @param lastUsed 最后使用时间
     */
    void updateLastUsed(String keyCode, Instant lastUsed);
}
