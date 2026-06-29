package com.codingas.gateway.domain.resilience.gateway;

import com.codingas.gateway.domain.resilience.entity.FailoverEvent;

import java.time.Instant;
import java.util.List;

/**
 * 转移事件领域网关接口
 *
 * <p>转移事件聚合根的持久化抽象。domain 层仅依赖此接口，
 * 实现位于 infrastructure 层（COLA Light 依赖倒置）。</p>
 *
 * <p>查询语义：</p>
 * <ul>
 *   <li>{@link #findRecent} — 按 occurredAt 倒序返回转移事件流，支持 since/applicationId/clusterId
 *       可选过滤。clusterId 过滤基于冗余 fromClusterId/toClusterId 字段匹配（任一命中即返回）；
 *       Invoker 通过 {@code ChannelGateway.findById} 反查 channelId→clusterId 填充冗余字段，
 *       clusterId 过滤已生效</li>
 *   <li>{@link #findExhausted} — 返回 exhausted=true 的耗尽告警事件，按 occurredAt 倒序</li>
 * </ul>
 */
public interface FailoverEventGateway {

    /**
     * 保存转移事件
     *
     * @param event 转移事件实体
     * @return 保存后的转移事件实体（含生成的 ID 与审计字段）
     */
    FailoverEvent save(FailoverEvent event);

    /**
     * 查询近期转移事件（按 occurredAt 倒序）
     *
     * @param applicationId 应用 ID 过滤（可空，空表示不过滤）
     * @param clusterId     故障域 ID 过滤（可空，空表示不过滤；非空时匹配冗余 fromClusterId/toClusterId）
     * @param since         起始时间过滤（可空，空表示不限起始时间）
     * @param limit         返回条数上限
     * @return 转移事件列表（按 occurredAt 倒序）
     */
    List<FailoverEvent> findRecent(Instant since, Long applicationId, Long clusterId, int limit);

    /**
     * 查询耗尽告警事件（exhausted=true，按 occurredAt 倒序）
     *
     * @param since 起始时间过滤（可空，空表示不限起始时间）
     * @param limit 返回条数上限
     * @return 耗尽事件列表（按 occurredAt 倒序）
     */
    List<FailoverEvent> findExhausted(Instant since, int limit);
}
