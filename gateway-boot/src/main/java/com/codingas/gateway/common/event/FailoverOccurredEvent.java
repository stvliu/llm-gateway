package com.codingas.gateway.common.event;

import com.codingas.gateway.domain.supply.enums.FailoverDecision;
import com.codingas.gateway.domain.supply.enums.ProviderErrorType;

import java.time.Instant;

/**
 * 转移发生领域事件
 *
 * <p>由 {@code ChannelFailoverInvoker} 在 catch 块判定 {@link FailoverDecision} 非 NONE
 * （L1/L2 换候选）时，换下一候选前发布。由 {@code FailoverEventListener}
 * （{@code @TransactionalEventListener(AFTER_COMMIT)}）异步接收并调
 * {@code FailoverEventGateway.save} 持久化为 {@code FailoverEvent} 实体。</p>
 *
 * <p><b>命名说明</b>：事件类命名为 {@code FailoverOccurredEvent}（转移发生事件），
 * 避免与持久化实体 {@code FailoverEvent}（转移事件）同名冲突。参照 {@link AuditEvent}
 * 的 record + implements {@link DomainEvent} + occurredOn() 范式。</p>
 *
 * <p>设计见 design doc D12：发布与持久化解耦，不阻塞 10k QPS 调用链。
 * 可靠性边界：发布后持久化前进程崩溃则事件丢失（可观测性数据可接受，非计费/审计关键路径）。</p>
 *
 * @param traceId          OpenTelemetry Trace ID，串联同请求多次转移
 * @param applicationId    应用 ID（权限锚点）
 * @param fromChannelId    失败候选的渠道 ID
 * @param fromEndpointId   失败候选的端点 ID
 * @param toChannelId      转移目标候选的渠道 ID（exhausted 时为 null）
 * @param toEndpointId     转移目标候选的端点 ID（exhausted 时为 null）
 * @param fromClusterId    冗余：失败候选所属故障域 ID（可空，当前 RoutingContext 未携带 clusterId 故为 null）
 * @param toClusterId      冗余：转移目标所属故障域 ID（可空，同上）
 * @param errorType        触发转移的上游错误类型
 * @param decision         转移决策（L1/L2）
 * @param exhausted        是否候选全部耗尽（to 为 null 时为 true）
 * @param occurredOn       转移发生时间
 */
public record FailoverOccurredEvent(
        String traceId,
        Long applicationId,
        Long fromChannelId,
        Long fromEndpointId,
        Long toChannelId,
        Long toEndpointId,
        Long fromClusterId,
        Long toClusterId,
        ProviderErrorType errorType,
        FailoverDecision decision,
        boolean exhausted,
        Instant occurredOn
) implements DomainEvent {

    @Override
    public Instant occurredOn() {
        return occurredOn;
    }
}
