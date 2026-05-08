package com.codingas.gateway.infrastructure.model.gateway.database;

import com.codingas.gateway.infrastructure.model.gateway.database.dataobject.ProviderApiKeyDo;
import com.codingas.gateway.infrastructure.model.gateway.database.dataobject.ProviderApiKeyDo.ProviderApiKeyStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
     * 根据 Provider ID 查找所有 Key
     */
    List<ProviderApiKeyDo> findByProviderId(Long providerId);

    /**
     * 根据 Provider ID 查找所有 Key（分页）
     */
    Page<ProviderApiKeyDo> findByProviderId(Long providerId, Pageable pageable);

    /**
     * 根据 Provider ID 和状态查找 Key（分页）
     */
    Page<ProviderApiKeyDo> findByProviderIdAndStatus(Long providerId, ProviderApiKeyStatus status, Pageable pageable);

    /**
     * 根据 Provider ID 和关键字查找 Key（分页）
     */
    @Query("SELECT k FROM ProviderApiKeyDo k WHERE k.providerId = :providerId AND LOWER(k.keyName) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<ProviderApiKeyDo> findByProviderIdAndKeyword(Long providerId, @Param("keyword") String keyword, Pageable pageable);

    /**
     * 根据 Provider ID、状态和关键字查找 Key（分页）
     */
    @Query("SELECT k FROM ProviderApiKeyDo k WHERE k.providerId = :providerId AND k.status = :status AND LOWER(k.keyName) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<ProviderApiKeyDo> findByProviderIdAndStatusAndKeyword(Long providerId, @Param("status") ProviderApiKeyStatus status, @Param("keyword") String keyword, Pageable pageable);

    /**
     * 根据 Provider ID 查找活跃 Key
     */
    @Query("SELECT k FROM ProviderApiKeyDo k WHERE k.providerId = :providerId " +
           "AND k.status IN ('ACTIVE', 'RATE_LIMITED', 'OVERQUOTA', 'ERROR') " +
           "AND (k.expiresAt IS NULL OR k.expiresAt > :now)")
    List<ProviderApiKeyDo> findActiveKeysByProviderId(@Param("providerId") Long providerId, @Param("now") Instant now);

    /**
     * 查找 Provider 的默认 Key
     */
    Optional<ProviderApiKeyDo> findByProviderIdAndIsDefaultTrue(Long providerId);

    /**
     * 统计 Provider 下的 Key 数量
     */
    long countByProviderId(Long providerId);

    /**
     * 更新 Key 状态
     */
    @Modifying
    @Query("UPDATE ProviderApiKeyDo k SET k.status = :status, k.disabledReason = :reason WHERE k.id = :id")
    void updateStatus(@Param("id") Long id, @Param("status") ProviderApiKeyStatus status,
                      @Param("reason") ProviderApiKeyDo.ProviderApiKeyDisabledReason reason);

    /**
     * 更新最后使用时间
     */
    @Modifying
    @Query("UPDATE ProviderApiKeyDo k SET k.lastUsedAt = :lastUsedAt WHERE k.id = :id")
    void updateLastUsedAt(@Param("id") Long id, @Param("lastUsedAt") Instant lastUsedAt);

    /**
     * 清除 Provider 下其他 Key 的默认标记
     */
    @Modifying
    @Query("UPDATE ProviderApiKeyDo k SET k.isDefault = false WHERE k.providerId = :providerId AND k.id != :excludeId")
    void clearDefaultFlagForOtherKeys(@Param("providerId") Long providerId, @Param("excludeId") Long excludeId);
}