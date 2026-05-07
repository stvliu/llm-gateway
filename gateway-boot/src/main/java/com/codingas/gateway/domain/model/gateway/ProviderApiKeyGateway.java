package com.codingas.gateway.domain.model.gateway;

import com.codingas.gateway.domain.model.entity.ProviderApiKey;
import com.codingas.gateway.domain.model.entity.ProviderApiKey.ProviderApiKeyStatus;
import com.codingas.gateway.domain.model.entity.ProviderApiKey.ProviderApiKeyDisabledReason;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * 提供商 API 密钥网关接口
 *
 * <p>定义在 domain 层，由 infrastructure 层实现。</p>
 * <p>Domain 不直接依赖持久化，通过此接口操作提供商 API 密钥。</p>
 */
public interface ProviderApiKeyGateway {

    /**
     * 根据密钥 ID 查找 API 密钥
     *
     * @param id 密钥 ID
     * @return API 密钥信息，不存在返回空
     */
    Optional<ProviderApiKey> findById(Long id);

    /**
     * 根据提供商 ID 查找 API 密钥（旧架构，向后兼容）
     *
     * @param providerId 提供商 ID
     * @return API 密钥信息，不存在返回空
     * @deprecated 请使用 findByChannelId
     */
    @Deprecated
    Optional<ProviderApiKey> findByProviderId(Long providerId);

    /**
     * 根据渠道 ID 查找所有 API 密钥
     *
     * @param channelId 渠道 ID
     * @return API 密钥列表
     */
    List<ProviderApiKey> findByChannelId(Long channelId);

    /**
     * 根据渠道 ID 查找活跃的 API 密钥
     *
     * <p>活跃状态包括：ACTIVE、RATE_LIMITED、OVERQUOTA、ERROR</p>
     *
     * @param channelId 渠道 ID
     * @return 活跃的 API 密钥列表
     */
    List<ProviderApiKey> findActiveKeysByChannelId(Long channelId);

    /**
     * 查找渠道的默认 API 密钥
     *
     * @param channelId 渠道 ID
     * @return 默认 API 密钥，不存在返回空
     */
    Optional<ProviderApiKey> findDefaultKeyByChannelId(Long channelId);

    /**
     * 统计渠道下的 API 密钥数量
     *
     * @param channelId 渠道 ID
     * @return 密钥数量
     */
    long countByChannelId(Long channelId);

    /**
     * 保存提供商 API 密钥
     *
     * @param providerApiKey API 密钥实体
     * @return 保存后的实体
     */
    ProviderApiKey save(ProviderApiKey providerApiKey);

    /**
     * 更新 API 密钥状态
     *
     * @param id 密钥 ID
     * @param status 新状态
     * @param reason 禁用原因（可为空）
     */
    void updateStatus(Long id, ProviderApiKeyStatus status, ProviderApiKeyDisabledReason reason);

    /**
     * 更新最后使用时间
     *
     * @param id 密钥 ID
     * @param lastUsedAt 最后使用时间
     */
    void updateLastUsedAt(Long id, Instant lastUsedAt);

    /**
     * 清除渠道下其他 Key 的默认标记
     *
     * @param channelId 渠道 ID
     * @param excludeId 排除的密钥 ID
     */
    void clearDefaultFlagForOtherKeys(Long channelId, Long excludeId);

    /**
     * 获取最大版本号
     *
     * <p>用于变更检测。</p>
     *
     * @return 最大版本号，无数据返回 0
     */
    default long getMaxVersion() {
        return 0L;
    }
}
