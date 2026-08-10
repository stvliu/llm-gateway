/*
 * Copyright (c) 2025 codingas.com
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE file for details.
 */
package com.codingas.gateway.infrastructure.supply.gateway.database.dataobject;

import com.codingas.gateway.domain.supply.enums.ChannelHealthSource;
import com.codingas.gateway.domain.supply.enums.ChannelHealthStatus;
import com.codingas.gateway.infrastructure.common.BaseDo;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.Instant;

/**
 * 渠道数据对象
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "channels")
public class ChannelDo extends BaseDo {

    @Column(name = "provider_id", nullable = false)
    private Long providerId;

    @Column(name = "name", nullable = false, length = 128)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "billing_mode", length = 32)
    private com.codingas.gateway.domain.supply.enums.BillingMode billingMode;

    @Column(name = "quota_limit")
    private Long quotaLimit;

    @Column(name = "timeout")
    private Integer timeout;

    @Column(name = "max_retries")
    private Integer maxRetries;

    @Column(name = "state", nullable = false, length = 32)
    private String state;

    /** 最近一次连通性测试完成时间（last-write-wins） */
    @Column(name = "last_health_check_at")
    private Instant lastHealthCheckAt;

    /** 最近一次健康聚合状态 */
    @Enumerated(EnumType.STRING)
    @Column(name = "last_health_status", length = 16)
    private ChannelHealthStatus lastHealthStatus;

    /** 最近一次测试触发来源（仅 CARD / DRAWER 持久化） */
    @Enumerated(EnumType.STRING)
    @Column(name = "last_health_source", length = 16)
    private ChannelHealthSource lastHealthSource;
}