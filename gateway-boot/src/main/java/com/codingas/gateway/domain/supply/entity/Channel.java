package com.codingas.gateway.domain.supply.entity;

import com.codingas.gateway.common.entity.BaseEntity;
import com.codingas.gateway.common.entity.DomainEntity;
import com.codingas.gateway.domain.supply.enums.BillingMode;
import com.codingas.gateway.domain.supply.enums.ChannelHealthSource;
import com.codingas.gateway.domain.supply.enums.ChannelHealthStatus;
import com.codingas.gateway.domain.supply.enums.ChannelState;
import lombok.Data;
import lombok.EqualsAndHashCode;


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
public class Channel extends BaseEntity {

    private Long providerId;

    private String name;

    /** 计费模式 */
    private BillingMode billingMode;

    /** 配额限制（Token 数） */
    private Long quotaLimit;

    private Integer timeout;

    private Integer maxRetries;

    private ChannelState state = ChannelState.PENDING;

    /** 最近一次连通性测试完成时间（last-write-wins，无版本锁） */
    private Instant lastHealthCheckAt;

    /** 最近一次健康聚合状态 */
    private ChannelHealthStatus lastHealthStatus;

    /** 最近一次测试触发来源（仅 CARD / DRAWER 持久化） */
    private ChannelHealthSource lastHealthSource;

    /** 所属故障域 ID（物理 ID，可空，无 FK 约束；关联 clusters.id） */
    private Long clusterId;

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
        return ChannelState.ACTIVE.equals(state) || ChannelState.DEPRECATED.equals(state);
    }
}
