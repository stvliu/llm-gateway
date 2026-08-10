/*
 * Copyright (c) 2025 codingas.com
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE file for details.
 */
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
 * <p>渠道是上游接入的聚合根，下挂多协议端点（{@link ChannelEndpoint}）、
 * 多凭证（{@link ChannelCredential}）与多模型实例（{@link ModelInstance}）。</p>
 *
 * <p>一个渠道可同时持有多个不同协议的端点（如同一上游同时提供 OpenAI 与 Anthropic 端点），
 * 同协议端点由唯一约束 {@code uk_channel_endpoint(channel_id, protocol)} 限制为至多一个；
 * 多协议需求在同一渠道内解决，无需为每种协议单独建渠道。</p>
 *
 * <p>定价信息由 {@link Model} 统一管理，{@link ModelInstance} 仅保留规格覆盖配置；
 * 渠道本身只持有连接、路由、计费与健康相关字段。</p>
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
