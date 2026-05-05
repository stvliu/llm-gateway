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

    private Long providerId;

    private String keyName;

    private String apiKey;

    private String encryptedApiKey;

    private Integer priority = 100;

    private ProviderApiKeyStatus status = ProviderApiKeyStatus.ACTIVE;

    private Instant lastUsedAt;

    private Instant expiresAt;

    public enum ProviderApiKeyStatus {
        /** 活跃 */
        ACTIVE,
        /** 已禁用 */
        DISABLED,
        /** 已过期 */
        EXPIRED,
        /** 已删除 */
        DELETED
    }

    /**
     * 检查凭证是否有效
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
}
