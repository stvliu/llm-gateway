package com.codingas.gateway.domain.model.entity;

import com.codingas.gateway.common.entity.DomainEntity;
import com.codingas.gateway.common.entity.BaseEntity;

import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;

/**
 * Provider 调用凭证实体
 *
 * <p>网关调用大模型 Provider 时的凭据，属于系统维度。</p>
 * <p>一个 Provider 可有多个 Key（主备/轮换），管理员配置。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@DomainEntity
@Slf4j
public class ProviderApiKey extends BaseEntity {

    /**
     * 关联的 Provider ID（旧架构，向后兼容）
     */
    private Long providerId;

    /**
     * 关联的渠道 ID（新架构）
     */
    private Long channelId;

    private String keyName;

    private String apiKey;

    private String encryptedApiKey;

    private Integer priority = 100;

    private Integer weight = 100;

    private Boolean isDefault = false;

    private ProviderApiKeyStatus status = ProviderApiKeyStatus.ACTIVE;

    private ProviderApiKeyDisabledReason disabledReason;

    private Instant lastUsedAt;

    private Instant expiresAt;

    /**
     * 连续失败次数（用于健康检测）
     */
    private Integer consecutiveFailures;

    /**
     * API Key 状态枚举
     */
    public enum ProviderApiKeyStatus {
        /** 活跃 */
        ACTIVE,
        /** 已禁用 */
        DISABLED,
        /** 已过期 */
        EXPIRED,
        /** 限流中 */
        RATE_LIMITED,
        /** 超额 */
        OVERQUOTA,
        /** 错误 */
        ERROR,
        /** 已删除 */
        DELETED
    }

    /**
     * API Key 禁用原因枚举
     */
    public enum ProviderApiKeyDisabledReason {
        /** 手动禁用 */
        MANUAL,
        /** 速率限制 */
        RATE_LIMIT,
        /** 配额超限 */
        QUOTA_EXCEEDED,
        /** 认证失败 */
        AUTH_FAILED,
        /** Key 过期 */
        EXPIRED,
        /** 提供商错误 */
        PROVIDER_ERROR
    }

    /**
     * 检查凭证是否可用
     *
     * <p>可用状态包括：ACTIVE、RATE_LIMITED（可恢复）、OVERQUOTA（可恢复）、ERROR（可恢复）</p>
     */
    public boolean isAvailable() {
        // 永久禁用状态不可用
        if (status == ProviderApiKeyStatus.DISABLED ||
            status == ProviderApiKeyStatus.EXPIRED ||
            status == ProviderApiKeyStatus.DELETED) {
            return false;
        }
        // 检查是否过期
        if (expiresAt != null && Instant.now().isAfter(expiresAt)) {
            return false;
        }
        return true;
    }

    /**
     * 检查凭证是否有效（仅 ACTIVE 状态）
     */
    public boolean isValid() {
        if (ProviderApiKeyStatus.ACTIVE.equals(status)) {
            if (expiresAt != null && Instant.now().isAfter(expiresAt)) {
                return false;
            }
            return true;
        }
        return false;
    }

    /**
     * 检查是否为临时禁用状态（可自动恢复）
     */
    public boolean isTemporarilyUnavailable() {
        return status == ProviderApiKeyStatus.RATE_LIMITED ||
               status == ProviderApiKeyStatus.OVERQUOTA ||
               status == ProviderApiKeyStatus.ERROR;
    }
}
