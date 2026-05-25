package com.codingas.gateway.domain.supply.entity;

import com.codingas.gateway.common.entity.BaseEntity;
import com.codingas.gateway.common.entity.DomainEntity;
import com.codingas.gateway.domain.supply.enums.BillingMode;
import com.codingas.gateway.domain.supply.enums.ChannelState;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;

/**
 * 渠道实体
 *
 * <p>一个渠道对应一个端点和一个协议，多协议需求通过建多个 Channel 解决。</p>
 * <p>定价下沉到 ChannelModel，Channel 只持有连接和路由相关字段。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@DomainEntity
@Slf4j
public class Channel extends BaseEntity {

    private Long providerId;

    private String name;

    /** 计费模式 */
    private BillingMode billingMode;

    /** 配额限制（Token 数） */
    private Long quotaLimit;

    private Integer priority;

    private Integer weight;

    private Integer timeout;

    private Integer maxRetries;

    private ChannelState state = ChannelState.ACTIVE;

    @Override
    public Instant getCreatedAt() {
        return createdAt;
    }

    @Override
    public Instant getUpdatedAt() {
        return updatedAt;
    }

    /**
     * 检查渠道是否可用
     */
    public boolean isAvailable() {
        return ChannelState.ACTIVE.equals(state);
    }
}