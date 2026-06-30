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
 *
 * <p><b>Task 6 变更</b>：新增 common_cause_skip 列（共因跳过标记，默认 false）。
 * 本任务仅加列，Task 9 填充判定逻辑。</p>
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

    /** 冗余：失败候选所属故障域 ID（可空） */
    @Column(name = "from_cluster_id")
    private Long fromClusterId;

    /** 冗余：转移目标所属故障域 ID（可空） */
    @Column(name = "to_cluster_id")
    private Long toClusterId;

    /** 触发转移的上游错误类型（枚举名） */
    @Column(name = "error_type", nullable = false, length = 32)
    private String errorType;

    /** 转移决策（L1 枚举名） */
    @Column(name = "decision", nullable = false, length = 8)
    private String decision;

    /** 是否候选全部耗尽 */
    @Column(name = "exhausted", nullable = false)
    private boolean exhausted;

    /** 是否共因跳过标记（默认 false，Task 9 填充） */
    @Column(name = "common_cause_skip", nullable = false)
    private boolean commonCauseSkip;

    /** 转移发生时间（业务时间，查询排序键） */
    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;
}
