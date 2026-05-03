package com.codingas.gateway.domain.model.entity;

import com.codingas.gateway.domain.DomainEntity;
import com.codingas.gateway.domain.BaseEntity;

import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;

/**
 * 渠道实体
 *
 * <p>Provider 调用实例，包含特定配置和健康状态。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@DomainEntity
@Slf4j
public class Channel extends BaseEntity {

    private String channelCode;

    private Long providerId;

    private String name;

    private String baseUrl;

    private Integer timeout;

    private Integer maxRetries;

    private Integer priority;

    private Integer weight;

    private ChannelStatus status = ChannelStatus.ACTIVE;

    private Instant lastHealthCheckAt;

    private Integer consecutiveFailures;

    public enum ChannelStatus {
        /** 活跃 */
        ACTIVE,
        /** 不健康 */
        UNHEALTHY,
        /** 已禁用 */
        DISABLED
    }

    /**
     * 检查渠道是否可用
     */
    public boolean isAvailable() {
        return ChannelStatus.ACTIVE.equals(status);
    }

    /**
     * 检查是否需要健康检查
     */
    public boolean needsHealthCheck() {
        return lastHealthCheckAt == null ||
            Instant.now().minusSeconds(300).isAfter(lastHealthCheckAt);
    }
}
