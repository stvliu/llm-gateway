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
package com.codingas.gateway.common.event;

import com.codingas.gateway.common.enums.FailoverDecision;
import com.codingas.gateway.common.enums.ProviderErrorType;

import java.time.Instant;

/**
 * 转移发生领域事件
 *
 * <p>由 {@code ChannelFailoverInvoker} 在 catch 块判定 {@link FailoverDecision} 非 NONE
 * （L1 换候选）时，换下一候选前发布。由 {@code FailoverEventListener}
 * （{@code @EventListener}，非事务监听）接收并调
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
 * @param errorType        触发转移的上游错误类型
 * @param decision         转移决策（L1）
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
