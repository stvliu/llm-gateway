package com.codingas.gateway.application.proxy.routing;

import com.codingas.gateway.domain.resilience.entity.ResilienceProfile;
import com.codingas.gateway.domain.supply.enums.Protocol;
import com.codingas.gateway.domain.supply.enums.RoutingStrategy;

/**
 * 路由请求上下文 — 携带 RouterChain 各环节所需的信息
 *
 * <p>权限锚点为 {@code applicationId}：数据面权限路由（{@link PermissionRouter}）
 * 依据应用-渠道授权（ApplicationChannel）判定可见渠道集合，{@code applicationId} 为 null
 * 时权限路由直接返回空集。</p>
 *
 * <p>{@code protocol} 为入站协议，供 {@link HealthRouter} 按协议从 channelId 派生 endpointId，
 * 统一熔断 key 为 endpoint 粒度（与 {@code KeyFailoverInvoker} 共享同一熔断器 bean）。</p>
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
     */
    private final ResilienceProfile resilienceProfile;

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
     * <p>委托 {@link #RoutingRequest(Long, Long, Long, String, RoutingStrategy, Protocol, ResilienceProfile)}
     * 传 null profile。</p>
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
        this(modelId, applicationId, userId, role, strategy, protocol, null);
    }

    /**
     * 构造路由请求上下文（携带容灾画像，Task 4.9）
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
        this.modelId = modelId;
        this.applicationId = applicationId;
        this.userId = userId;
        this.role = role;
        this.strategy = strategy;
        this.protocol = protocol;
        this.resilienceProfile = resilienceProfile;
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
}
