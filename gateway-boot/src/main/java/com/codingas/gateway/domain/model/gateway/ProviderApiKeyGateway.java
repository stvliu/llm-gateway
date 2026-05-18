package com.codingas.gateway.domain.model.gateway;

import com.codingas.gateway.domain.model.entity.ProviderApiKey;
import com.codingas.gateway.domain.model.enums.ProviderApiKeyState;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * 提供商 API 密钥网关接口
 *
 * <p>定义在 domain 层，由 infrastructure 层实现。</p>
 * <p>Domain 不直接依赖持久化，通过此接口操作提供商 API 密钥。</p>
 */
/**
 * @deprecated 旧架构 Gateway，由 ProductApiKeyGateway 替代
 */
@Deprecated(since = "2.0", forRemoval = true)
public interface ProviderApiKeyGateway {

    /**
     * 根据密钥 ID 查找 API 密钥
     */
    Optional<ProviderApiKey> findById(Long id);

    /**
     * 根据 Provider ID 查找所有 API 密钥
     */
    List<ProviderApiKey> findByProviderId(Long providerId);

    /**
     * 根据 Provider ID 查找所有 API 密钥（分页）
     */
    Page<ProviderApiKey> findByProviderId(Long providerId, Pageable pageable);

    /**
     * 根据 Provider ID 和状态查找 API 密钥（分页）
     */
    Page<ProviderApiKey> findByProviderIdAndState(Long providerId, ProviderApiKeyState state, Pageable pageable);

    /**
     * 根据 Provider ID 和关键字查找 API 密钥（分页）
     */
    Page<ProviderApiKey> findByProviderIdAndKeyword(Long providerId, String keyword, Pageable pageable);

    /**
     * 根据 Provider ID、状态和关键字查找 API 密钥（分页）
     */
    Page<ProviderApiKey> findByProviderIdAndStateAndKeyword(Long providerId, ProviderApiKeyState state, String keyword, Pageable pageable);

    /**
     * 根据 Provider ID 查找活跃的 API 密钥
     *
     * <p>活跃状态：ACTIVE</p>
     */
    List<ProviderApiKey> findActiveKeysByProviderId(Long providerId);

    /**
     * 查找 Provider 的默认 API 密钥
     */
    Optional<ProviderApiKey> findDefaultKeyByProviderId(Long providerId);

    /**
     * 统计 Provider 下的 API 密钥数量
     */
    long countByProviderId(Long providerId);

    /**
     * 保存提供商 API 密钥
     */
    ProviderApiKey save(ProviderApiKey providerApiKey);

    /**
     * 更新 API 密钥状态
     */
    void updateState(Long id, ProviderApiKeyState state);

    /**
     * 更新最后使用时间
     */
    void updateLastUsedAt(Long id, Instant lastUsedAt);

    /**
     * 清除 Provider 下其他 Key 的默认标记
     */
    void clearDefaultFlagForOtherKeys(Long providerId, Long excludeId);

    /**
     * 批量获取 Provider 的 Key 统计信息
     *
     * @param providerIds Provider ID 列表
     * @return Map&lt;providerId, KeyStats&gt;
     */
    java.util.Map<Long, com.codingas.gateway.application.provider.dto.ProviderKeyStats> getKeyStatsByProviderIds(java.util.List<Long> providerIds);

    /**
     * 获取最大版本号
     */
    default long getMaxVersion() {
        return 0L;
    }

    /**
     * 删除 API 密钥
     */
    void delete(ProviderApiKey providerApiKey);

    /**
     * 根据 ID 删除 API 密钥
     */
    void deleteById(Long id);
}
