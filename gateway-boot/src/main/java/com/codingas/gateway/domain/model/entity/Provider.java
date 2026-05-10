package com.codingas.gateway.domain.model.entity;
import com.codingas.gateway.common.entity.DomainEntity;
import com.codingas.gateway.common.entity.BaseEntity;

import com.codingas.gateway.domain.model.enums.ProviderType;
import com.codingas.gateway.domain.model.enums.ProviderState;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

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
    private String name;

    private ProviderType type;

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

    private ProviderState state = ProviderState.ACTIVE;

    /**
     * 检查提供商是否可用
     */
    public boolean isAvailable() {
        return ProviderState.ACTIVE.equals(state);
    }
}
