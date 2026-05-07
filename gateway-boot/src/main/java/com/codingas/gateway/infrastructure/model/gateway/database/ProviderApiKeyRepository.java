package com.codingas.gateway.infrastructure.model.gateway.database;

import com.codingas.gateway.infrastructure.model.gateway.database.dataobject.ProviderApiKeyDo;
import com.codingas.gateway.infrastructure.model.gateway.database.dataobject.ProviderApiKeyDo.ProviderApiKeyStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * 提供商 API 密钥仓储接口
 */
@Repository
public interface ProviderApiKeyRepository extends JpaRepository<ProviderApiKeyDo, Long> {

    /**
     * 根据提供商 ID 查找（旧架构，向后兼容）
     */
    Optional<ProviderApiKeyDo> findByProviderId(Long providerId);

    /**
     * 根据渠道 ID 查找所有 Key
     */
    List<ProviderApiKeyDo> findByChannelId(Long channelId);

    /**
     * 根据渠道 ID 查找活跃 Key
     */
    @Query("SELECT k FROM ProviderApiKeyDo k WHERE k.channelId = :channelId " +
           "AND k.status IN ('ACTIVE', 'RATE_LIMITED', 'OVERQUOTA', 'ERROR') " +
           "AND (k.expiresAt IS NULL OR k.expiresAt > :now)")
    List<ProviderApiKeyDo> findActiveKeysByChannelId(@Param("channelId") Long channelId, @Param("now") Instant now);

    /**
     * 查找渠道的默认 Key
     */
    Optional<ProviderApiKeyDo> findByChannelIdAndIsDefaultTrue(Long channelId);

    /**
     * 统计渠道下的 Key 数量
     */
    long countByChannelId(Long channelId);

    /**
     * 更新 Key 状态
     */
    @Modifying
    @Query("UPDATE ProviderApiKeyDo k SET k.status = :status, k.disabledReason = :reason WHERE k.id = :id")
    void updateStatus(@Param("id") Long id, @Param("status") ProviderApiKeyStatus status,
                      @Param("reason") com.codingas.gateway.infrastructure.model.gateway.database.dataobject.ProviderApiKeyDo.ProviderApiKeyDisabledReason reason);

    /**
     * 更新最后使用时间
     */
    @Modifying
    @Query("UPDATE ProviderApiKeyDo k SET k.lastUsedAt = :lastUsedAt WHERE k.id = :id")
    void updateLastUsedAt(@Param("id") Long id, @Param("lastUsedAt") Instant lastUsedAt);

    /**
     * 清除渠道下其他 Key 的默认标记
     */
    @Modifying
    @Query("UPDATE ProviderApiKeyDo k SET k.isDefault = false WHERE k.channelId = :channelId AND k.id != :excludeId")
    void clearDefaultFlagForOtherKeys(@Param("channelId") Long channelId, @Param("excludeId") Long excludeId);
}
