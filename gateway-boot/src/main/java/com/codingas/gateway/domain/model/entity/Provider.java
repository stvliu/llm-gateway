package com.codingas.gateway.domain.model.entity;
import com.codingas.gateway.common.entity.DomainEntity;
import com.codingas.gateway.common.entity.BaseEntity;

import com.codingas.gateway.common.enums.ProviderType;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;

/**
 * 提供商实体
 *
 * <p>表示 AI 模型服务提供商，如 OpenAI、Anthropic、智谱等。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@DomainEntity
@Slf4j
public class Provider extends BaseEntity {

    private String providerName;

    private ProviderType providerType;

    private String baseUrl;

    private String websiteUrl;

    private String apiDocUrl;

    private Integer priority = 100;

    /**
     * 调用超时时间（毫秒）
     */
    private Integer timeout = 30000;

    /**
     * 最大重试次数
     */
    private Integer maxRetries = 3;

    private ProviderStatus status = ProviderStatus.ACTIVE;

    private Instant deletedAt;

    public enum ProviderStatus {
        /** 正常 */
        ACTIVE,
        /** 暂停 */
        SUSPENDED,
        /** 已删除 */
        DELETED
    }

    /**
     * 检查提供商是否可用
     */
    public boolean isAvailable() {
        return ProviderStatus.ACTIVE.equals(status);
    }
}
