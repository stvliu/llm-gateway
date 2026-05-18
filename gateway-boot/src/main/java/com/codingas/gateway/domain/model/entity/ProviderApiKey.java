package com.codingas.gateway.domain.model.entity;

import com.codingas.gateway.common.entity.DomainEntity;
import com.codingas.gateway.common.entity.BaseEntity;
import com.codingas.gateway.domain.model.enums.ProviderApiKeyState;

import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;

/**
 * Provider 调用凭证实体
 *
 * <p>网关调用大模型 Provider 时的凭据，属于系统维度。</p>
 * <p>一个 Provider 可有多个 Key（主备/轮换），管理员配置。</p>
 *
 * <h3>状态说明</h3>
 * <ul>
 *   <li>生命周期状态（{@link ProviderApiKeyState}）：持久化，管理员操作</li>
 *   <li>运行时健康状态：由熔断器管理，不持久化</li>
 * </ul>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@DomainEntity
@Slf4j
/**
 * @deprecated 旧架构供应商 API Key 实体，由 ProductApiKey 替代
 */
@Deprecated(since = "2.0", forRemoval = true)
public class ProviderApiKey extends BaseEntity {

    /**
     * 关联的 Provider ID
     */
    private Long providerId;

    private String keyName;

    /**
     * Provider API Key（明文）
     * <p>在 Domain 层以明文形式存在，由 Infrastructure 层负责加密存储和解密读取。</p>
     */
    private String apiKey;

    private Integer priority = 100;

    private Integer weight = 100;

    private Boolean isDefault = false;

    private ProviderApiKeyState state = ProviderApiKeyState.ACTIVE;

    private Instant lastUsedAt;

    /**
     * 每分钟请求数限制
     */
    private Integer rpmLimit;

    /**
     * 每分钟 Token 数限制
     */
    private Long tpmLimit;

    /**
     * 连续失败次数（用于健康检测）
     */
    private Integer consecutiveFailures;

    /**
     * 检查凭证是否可用
     */
    public boolean isAvailable() {
        return state.isAvailable();
    }

    /**
     * 检查凭证是否有效（仅 ACTIVE 状态）
     */
    public boolean isValid() {
        return state == ProviderApiKeyState.ACTIVE;
    }
}
