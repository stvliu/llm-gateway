package com.codingas.gateway.domain.resilience.entity;

import com.codingas.gateway.common.entity.BaseEntity;
import com.codingas.gateway.common.entity.DomainEntity;
import com.codingas.gateway.domain.supply.enums.FailoverDecision;
import com.codingas.gateway.domain.supply.enums.ProviderErrorType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 转移事件聚合根实体
 *
 * <p>记录每次候选转移（容灾可观测性，读侧重）。当 {@code ChannelFailoverInvoker} 在 catch 块
 * 判定 {@link FailoverDecision} 非 NONE（L1 换候选）时，换下一候选前发布
 * {@code FailoverOccurredEvent}，由 {@code FailoverEventListener} 异步持久化本实体。</p>
 *
 * <p>设计见 design doc D12：独立 FailoverEvent domain，不复用 CallLog（调用结果语义与转移动作
 * 语义不同维度）。用途是容灾总览页 10s 轮询渲染转移事件流 + 耗尽告警。</p>
 *
 * <p>领域模型纯洁：仅含 Getter/Setter，不含业务逻辑。</p>
 *
 * <p>字段说明：</p>
 * <ul>
 *   <li>traceId — OpenTelemetry Trace ID，串联同请求多次转移</li>
 *   <li>applicationId — 应用 ID（权限锚点，过滤维度）</li>
 *   <li>fromChannelId / fromEndpointId — 失败候选的渠道 ID 与端点 ID</li>
 *   <li>toChannelId / toEndpointId — 转移目标候选的渠道 ID 与端点 ID；
 *       已是最后一个候选（exhausted=true）时为 null</li>
 *   <li>fromClusterId / toClusterId — 冗余故障域 ID（可空，便于 findRecent 的 clusterId 过滤直接匹配，
 *       避免 join channels 表）。Invoker 经 {@code ChannelGateway.findById} 反查 channelId→clusterId 填充，
 *       渠道不存在或未关联 cluster 时为 null；clusterId 过滤已生效</li>
 *   <li>errorType — 触发转移的上游错误类型</li>
 *   <li>decision — 转移决策（L1 换渠道共因故障）</li>
 *   <li>exhausted — 是否候选全部耗尽（to 为 null 时为 true）</li>
 *   <li>commonCauseSkip — 是否共因跳过标记（默认 false，Task 9 填充判定逻辑）</li>
 *   <li>occurredAt — 转移发生时间（查询排序键，倒序）</li>
 *   <li>id, createdBy, createdAt, updatedBy, updatedAt — 主键与审计字段，继承自 {@link BaseEntity}</li>
 * </ul>
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@DomainEntity
public class FailoverEvent extends BaseEntity {

    /** OpenTelemetry Trace ID，串联同请求多次转移 */
    private String traceId;

    /** 应用 ID（权限锚点，过滤维度） */
    private Long applicationId;

    /** 失败候选的渠道 ID */
    private Long fromChannelId;

    /** 失败候选的端点 ID */
    private Long fromEndpointId;

    /** 转移目标候选的渠道 ID（exhausted 时为 null） */
    private Long toChannelId;

    /** 转移目标候选的端点 ID（exhausted 时为 null） */
    private Long toEndpointId;

    /** 冗余：失败候选所属故障域 ID（可空，便于 clusterId 过滤） */
    private Long fromClusterId;

    /** 冗余：转移目标所属故障域 ID（可空，便于 clusterId 过滤） */
    private Long toClusterId;

    /** 触发转移的上游错误类型 */
    private ProviderErrorType errorType;

    /** 转移决策（L1） */
    private FailoverDecision decision;

    /** 是否候选全部耗尽（to 为 null 时为 true） */
    private boolean exhausted;

    /** 是否共因跳过标记（默认 false，Task 9 填充判定逻辑） */
    private boolean commonCauseSkip;

    /** 转移发生时间（查询排序键，倒序） */
    private Instant occurredAt;
}
