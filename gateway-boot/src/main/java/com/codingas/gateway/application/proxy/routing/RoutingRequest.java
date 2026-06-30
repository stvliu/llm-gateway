package com.codingas.gateway.application.proxy.routing;

import com.codingas.gateway.domain.resilience.entity.ResilienceProfile;
import com.codingas.gateway.domain.supply.enums.Protocol;
import com.codingas.gateway.domain.supply.enums.RoutingStrategy;

import java.util.Map;

/**
 * 路由请求上下文 — 携带 RouterChain 各环节所需的信息
 *
 * <p>权限锚点为 {@code applicationId}：数据面权限路由（{@link PermissionRouter}）
 * 依据应用-渠道授权（ApplicationChannel）判定可见渠道集合，{@code applicationId} 为 null
 * 时权限路由直接返回空集。</p>
 *
 * <p>{@code protocol} 为入站协议，供 {@link HealthRouter} 按协议从 channelId 派生 endpointId，
 * 统一熔断 key 为 endpoint 粒度（与 {@code KeyFailoverInvoker} 共享同一熔断器 bean）。</p>
 *
 * <p>Task 3：{@code channelPriorityMap} 携带应用级渠道转移优先级（key=channelId, value=priority），
 * 供 {@link PriorityRouter} 按应用级 priority 升序排序，实现同一渠道对不同应用不同转移顺序。
 * 为空表示无应用级映射（{@code PriorityRouter} 回退默认值 100）。</p>
 */
public class RoutingRequest {

    private final Long modelId;
    private final Long applicationId;
    private final Long userId;
    private final String role;
    private final RoutingStrategy strategy;
    private final Protocol protocol;
    /**
     * 容灾画像（Task 4.9 贯穿）：贯穿 RouterChain 与 Invoker 链，供
     * {@link PinnedModelRouter} 模型锁定、{@code ChannelFailoverInvoker} L2 门禁、
     * 会话亲和/就近等画像化决策。为 null 表示无画像（回退默认行为）。
     *
     * <p>注：Task 8 将移除该字段，本任务保留以维持兼容。</p>
     */
    private final ResilienceProfile resilienceProfile;
    /**
     * 应用级渠道转移优先级映射（key=channelId, value=priority，数值越小越优先）
     *
     * <p>由 {@link InstanceSelector} 查 {@code ApplicationChannelGateway.findByApplicationId}
     * 构建；为空（如 applicationId 为 null）时 {@link PriorityRouter} 回退默认值 100。</p>
     */
    private final Map<Long, Integer> channelPriorityMap;

    /**
     * @deprecated 请改用 {@link #RoutingRequest(Long, Long, Long, String, RoutingStrategy, Protocol)}。
     * 旧调用方未传入应用 ID，将导致 {@link PermissionRouter} 无权限锚点而返回空集。
     */
    @Deprecated
    public RoutingRequest(Long modelId, Long userId, String role) {
        this(modelId, null, userId, role, RoutingStrategy.WEIGHTED, null);
    }

    /**
     * @deprecated 请改用 {@link #RoutingRequest(Long, Long, Long, String, RoutingStrategy, Protocol)}。
     * 旧调用方未传入应用 ID，将导致 {@link PermissionRouter} 无权限锚点而返回空集。
     */
    @Deprecated
    public RoutingRequest(Long modelId, Long userId, String role, RoutingStrategy strategy) {
        this(modelId, null, userId, role, strategy, null);
    }

    /**
     * @deprecated 请改用 {@link #RoutingRequest(Long, Long, Long, String, RoutingStrategy, Protocol)}。
     * 本构造器未传入入站协议，{@link HealthRouter} 无法派生 endpointId，将把实例视为不可用而全部过滤。
     */
    @Deprecated
    public RoutingRequest(Long modelId, Long applicationId, Long userId, String role, RoutingStrategy strategy) {
        this(modelId, applicationId, userId, role, strategy, null);
    }

    /**
     * 构造路由请求上下文（无画像，向后兼容）
     *
     * <p>委托 {@link #RoutingRequest(Long, Long, Long, String, RoutingStrategy, Protocol, ResilienceProfile, Map)}
     * 传 null profile 与空 channelPriorityMap。</p>
     *
     * @param modelId       模型 ID
     * @param applicationId 应用 ID（权限锚点；为 null 时权限路由返回空集）
     * @param userId        用户 ID
     * @param role          用户角色
     * @param strategy      路由策略
     * @param protocol      入站协议（供 HealthRouter 按 channelId 派生 endpointId，统一熔断 key）
     */
    public RoutingRequest(Long modelId, Long applicationId, Long userId, String role,
                          RoutingStrategy strategy, Protocol protocol) {
        this(modelId, applicationId, userId, role, strategy, protocol, null, Map.of());
    }

    /**
     * 构造路由请求上下文（携带容灾画像，向后兼容）
     *
     * <p>委托 {@link #RoutingRequest(Long, Long, Long, String, RoutingStrategy, Protocol, ResilienceProfile, Map)}
     * 传空 channelPriorityMap（Task 3 之前的调用方无应用级映射，PriorityRouter 回退默认值 100）。</p>
     *
     * @param modelId          模型 ID
     * @param applicationId    应用 ID（权限锚点；为 null 时权限路由返回空集）
     * @param userId           用户 ID
     * @param role             用户角色
     * @param strategy         路由策略
     * @param protocol         入站协议（供 HealthRouter 按 channelId 派生 endpointId，统一熔断 key）
     * @param resilienceProfile 容灾画像（贯穿 RouterChain/Invoker 链；为 null 表示无画像回退默认行为）
     */
    public RoutingRequest(Long modelId, Long applicationId, Long userId, String role,
                          RoutingStrategy strategy, Protocol protocol, ResilienceProfile resilienceProfile) {
        this(modelId, applicationId, userId, role, strategy, protocol, resilienceProfile, Map.of());
    }

    /**
     * 构造路由请求上下文（携带容灾画像与应用级渠道优先级映射，Task 3）
     *
     * @param modelId           模型 ID
     * @param applicationId     应用 ID（权限锚点；为 null 时权限路由返回空集）
     * @param userId            用户 ID
     * @param role              用户角色
     * @param strategy          路由策略
     * @param protocol          入站协议（供 HealthRouter 按 channelId 派生 endpointId，统一熔断 key）
     * @param resilienceProfile 容灾画像（贯穿 RouterChain/Invoker 链；为 null 表示无画像回退默认行为）
     * @param channelPriorityMap 应用级渠道转移优先级映射（key=channelId, value=priority；为空回退默认值 100）
     */
    public RoutingRequest(Long modelId, Long applicationId, Long userId, String role,
                          RoutingStrategy strategy, Protocol protocol, ResilienceProfile resilienceProfile,
                          Map<Long, Integer> channelPriorityMap) {
        this.modelId = modelId;
        this.applicationId = applicationId;
        this.userId = userId;
        this.role = role;
        this.strategy = strategy;
        this.protocol = protocol;
        this.resilienceProfile = resilienceProfile;
        this.channelPriorityMap = channelPriorityMap != null ? channelPriorityMap : Map.of();
    }

    public Long getModelId() { return modelId; }

    public Long getApplicationId() { return applicationId; }

    public Long getUserId() { return userId; }

    public String getRole() { return role; }

    public RoutingStrategy getStrategy() { return strategy; }

    public Protocol getProtocol() { return protocol; }

    /**
     * @return 容灾画像；为 null 表示无画像（回退默认行为）
     */
    public ResilienceProfile getResilienceProfile() { return resilienceProfile; }

    /**
     * 返回应用级渠道转移优先级映射（key=channelId, value=priority，数值越小越优先）
     *
     * @return 渠道优先级映射（不应被调用方修改）；为空表示无应用级映射，{@link PriorityRouter} 回退默认值 100
     */
    public Map<Long, Integer> getChannelPriorityMap() { return channelPriorityMap; }
}
