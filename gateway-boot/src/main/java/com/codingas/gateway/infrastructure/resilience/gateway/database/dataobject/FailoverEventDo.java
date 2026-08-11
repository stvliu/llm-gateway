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
package com.codingas.gateway.infrastructure.resilience.gateway.database.dataobject;

import com.codingas.gateway.infrastructure.common.BaseDo;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.Instant;

/**
 * 转移事件数据对象
 *
 * <p>对应 failover_events 表；主键与审计字段（created_by/created_at/updated_by/updated_at）
 * 继承自 {@link BaseDo}，由 AuditingEntityListener 自动填充。</p>
 *
 * <p>error_type / decision 字段以字符串存储（枚举名），由 Gateway 实现层在 DO↔Entity 转换时
 * 还原为 {@link com.codingas.gateway.domain.supply.enums.ProviderErrorType} /
 * {@link com.codingas.gateway.domain.supply.enums.FailoverDecision} 枚举。</p>
 *
 * <p>occurred_at 为业务时间（转移发生时刻），独立于审计字段 created_at（持久化时刻）。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "failover_events")
public class FailoverEventDo extends BaseDo {

    /** OpenTelemetry Trace ID */
    @Column(name = "trace_id", length = 64)
    private String traceId;

    /** 应用 ID */
    @Column(name = "application_id")
    private Long applicationId;

    /** 失败候选的渠道 ID */
    @Column(name = "from_channel_id")
    private Long fromChannelId;

    /** 失败候选的端点 ID */
    @Column(name = "from_endpoint_id")
    private Long fromEndpointId;

    /** 转移目标候选的渠道 ID（exhausted 时为 null） */
    @Column(name = "to_channel_id")
    private Long toChannelId;

    /** 转移目标候选的端点 ID（exhausted 时为 null） */
    @Column(name = "to_endpoint_id")
    private Long toEndpointId;

    /** 触发转移的上游错误类型（枚举名） */
    @Column(name = "error_type", nullable = false, length = 32)
    private String errorType;

    /** 转移决策（L1 枚举名） */
    @Column(name = "decision", nullable = false, length = 8)
    private String decision;

    /** 是否候选全部耗尽 */
    @Column(name = "exhausted", nullable = false)
    private boolean exhausted;

    /** 转移发生时间（业务时间，查询排序键） */
    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;
}
