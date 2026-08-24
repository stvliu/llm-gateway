/*
 * Copyright © 2025-2026 codingas.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.codingas.gateway.providerdata.dataobject;

import com.codingas.gateway.provider.channel.ChannelHealthSource;
import com.codingas.gateway.provider.channel.ChannelHealthStatus;
import com.codingas.gateway.common.data.BaseDo;
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
    private com.codingas.gateway.provider.model.BillingMode billingMode;

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