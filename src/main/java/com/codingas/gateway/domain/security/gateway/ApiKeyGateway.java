package com.codingas.gateway.domain.security.gateway;

import com.codingas.gateway.domain.security.entity.GatewayApiKey;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;

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
     * 根据用户 ID 查找所有密钥
     *
     * @param userId 用户 ID
     * @return 密钥列表
     */
    List<GatewayApiKey> findByUserId(Long userId);

    /**
     * 查询即将过期的 API Key
     *
     * @param now 当前时间
     * @param threshold 过期阈值时间
     * @param pageable 分页
     * @return 即将过期的密钥分页
     */
    Page<GatewayApiKey> findExpiringKeys(Instant now, Instant threshold, Pageable pageable);

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
    void updateLastUsed(String keyCode, Instant lastUsed);
}