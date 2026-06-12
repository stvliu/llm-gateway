package com.codingas.gateway.domain.supply.entity;

import com.codingas.gateway.common.entity.BaseEntity;
import com.codingas.gateway.common.entity.DomainEntity;
import com.codingas.gateway.domain.supply.enums.BillingMode;
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

    /**
     * 渠道生命周期状态
     */
    public enum State {
        PENDING,
        ACTIVE,
        SUSPENDED,
        DEPRECATED,
        RETIRED;

        public boolean isRoutable() {
            return this == ACTIVE || this == DEPRECATED;
        }

        public boolean isTerminal() {
            return this == RETIRED;
        }

        public boolean canTransitionTo(State target) {
            return switch (this) {
                case PENDING    -> target == ACTIVE;
                case ACTIVE     -> target == SUSPENDED || target == DEPRECATED;
                case SUSPENDED  -> target == ACTIVE    || target == DEPRECATED || target == RETIRED;
                case DEPRECATED -> target == RETIRED;
                case RETIRED    -> false;
            };
        }
    }

    private Long providerId;

    private String name;

    /** 计费模式 */
    private BillingMode billingMode;

    /** 配额限制（Token 数） */
    private Long quotaLimit;

    private Integer timeout;

    private Integer maxRetries;

    private State state = State.PENDING;

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
        return State.ACTIVE.equals(state) || State.DEPRECATED.equals(state);
    }
}