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
package com.codingas.gateway.application.proxy.routing;

import com.codingas.gateway.common.exception.ResourceNotFoundException;
import com.codingas.gateway.domain.application.entity.Application;
import com.codingas.gateway.domain.application.enums.FailureStrategy;
import com.codingas.gateway.domain.application.gateway.ApplicationGateway;
import com.codingas.gateway.domain.supply.entity.Channel;
import com.codingas.gateway.domain.supply.entity.ChannelEndpoint;
import com.codingas.gateway.domain.supply.entity.ModelInstance;
import com.codingas.gateway.domain.supply.entity.Model;
import com.codingas.gateway.domain.supply.enums.Protocol;
import com.codingas.gateway.domain.supply.enums.RoutingStrategy;
import com.codingas.gateway.domain.supply.gateway.ChannelGateway;
import com.codingas.gateway.domain.supply.valueobject.RoutingContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 路由解析门面 — 编排四个子组件，组装 RoutingContext
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class RoutingResolver {

    private final ModelMatcher modelMatcher;
    private final InstanceSelector instanceSelector;
    private final CredentialResolver credentialResolver;
    private final EndpointResolver endpointResolver;
    private final ChannelGateway channelGateway;
    private final ApplicationGateway applicationGateway;

    /**
     * 根据模型名称解析完整的路由上下文（取最高优先级候选）
     *
     * <p>委托 {@link #resolveCandidates} 取候选列表的第一个，等价于"最高优先级候选"。
     * 候选列表为空时 {@link InstanceSelector#select} 已抛 {@link ResourceNotFoundException}，
     * 因此 getFirst() 调用安全。</p>
     *
     * @param modelName     模型名称
     * @param protocol      入站协议
     * @param applicationId 应用 ID（数据面权限锚点，透传至 InstanceSelector/RoutingRequest）
     * @param userId        用户 ID
     * @param role          用户角色（保留字段；数据面权限基于 applicationId 判定可见渠道）
     * @param strategy      路由策略
     * @return 路由上下文（最高优先级候选）
     */
    public RoutingContext resolve(String modelName, Protocol protocol, Long applicationId, Long userId, String role, RoutingStrategy strategy) {
        return resolveCandidates(modelName, protocol, applicationId, userId, role, strategy).getFirst();
    }

    /**
     * 根据模型名称解析路由候选列表，供 L1 渠道级故障转移逐个尝试
     *
     * <p>委托 {@link InstanceSelector#select} 拿到按 priority 升序的候选实例列表，
     * 逐个执行凭证解析 + 端点解析 + 渠道查询并组装 {@link RoutingContext}。
     * 返回列表顺序与候选 priority 一致（由 InstanceSelector 保证）。</p>
     *
     * @param modelName     模型名称
     * @param protocol      入站协议
     * @param applicationId 应用 ID（数据面权限锚点，透传至 InstanceSelector/RoutingRequest）
     * @param userId        用户 ID
     * @param role          用户角色
     * @param strategy      路由策略
     * @return 按 priority 升序的路由上下文候选列表（顺序与 InstanceSelector 候选一致）
     * @throws ResourceNotFoundException 无可用实例（由 InstanceSelector 透传）
     */
    public List<RoutingContext> resolveCandidates(String modelName, Protocol protocol, Long applicationId,
                                                  Long userId, String role, RoutingStrategy strategy) {
        // 1. 模型匹配
        Model model = modelMatcher.match(modelName);

        // 2. 实例选择 — 返回按 priority 升序的候选列表（applicationId 作为权限锚点、protocol 供 HealthRouter 派生 endpointId）
        List<ModelInstance> candidates = instanceSelector.select(model.getId(), applicationId, userId, role, strategy, protocol);

        // 3. 应用级配置解析 — applicationId 非 null 时查 Application 取 timeout 与 failureStrategy（避免每请求重复查 DB）
        Application app = resolveApplication(applicationId);

        // 4. 逐个候选转 RoutingContext（顺序保持 InstanceSelector 的 priority 升序，供 L1 故障转移逐个尝试）
        return candidates.stream()
                .map(instance -> buildContext(instance, model, protocol, app))
                .toList();
    }

    /**
     * 解析应用级配置（超时 + 失败处理策略）
     *
     * <p>从 {@link Application} 读取应用级 timeout 与 failureStrategy，承接原 ResilienceProfile 语义。
     * 配置查询不应阻断路由，故下列情况返回 null（交由 {@link #buildContext} 回退渠道默认/默认策略）：</p>
     * <ul>
     *   <li>applicationId 为 null（无应用锚点）</li>
     *   <li>Application 查不到（findById 返回 null，仅告警不抛异常）</li>
     * </ul>
     *
     * @param applicationId 应用 ID（可能为 null）
     * @return Application 实体；null 表示无应用锚点或查不到
     */
    private Application resolveApplication(Long applicationId) {
        if (applicationId == null) {
            return null;
        }
        Application app = applicationGateway.findById(applicationId);
        if (app == null) {
            log.warn("Application 未找到，超时与策略回退默认。applicationId={}", applicationId);
            return null;
        }
        return app;
    }

    /**
     * 组装单个候选实例的 RoutingContext
     *
     * <p>对单个候选实例执行凭证解析 + 端点解析 + 通道查询，并组装为 {@link RoutingContext}。</p>
     *
     * <p><b>有效超时 effectiveTimeout：</b>Application.timeout 非 0 覆盖渠道默认，为 null/0 时用渠道默认
     * （保留渠道 timeout 为 null 的语义，交由 Invoker 兜底默认值）。</p>
     *
     * <p><b>失败处理策略 strategy：</b>Application.failureStrategy 非 null 时透传，为 null（含 app 为 null）时
     * 回退默认 {@link FailureStrategy#FAIL_RETRY}。</p>
     *
     * @param instance 候选实例
     * @param model    所属模型
     * @param protocol 入站协议
     * @param app      应用实体（null 表示无应用锚点或查不到，超时与策略回退默认）
     * @return 路由上下文
     * @throws ResourceNotFoundException 渠道不存在
     */
    private RoutingContext buildContext(ModelInstance instance, Model model, Protocol protocol, Application app) {
        // 凭证解析
        String apiKey = credentialResolver.resolve(instance.getChannelId());

        // 端点解析（优先匹配协议同源）
        ChannelEndpoint endpoint = endpointResolver.resolve(instance.getChannelId(), protocol);

        // 渠道信息
        Channel channel = channelGateway.findById(instance.getChannelId())
                .orElseThrow(() -> new ResourceNotFoundException("Channel", instance.getChannelId()));

        // 有效超时：Application.timeout 非 0 覆盖渠道默认；app 为 null 或 timeout 为 0 时用渠道默认
        Integer effectiveTimeout = (app != null && app.getTimeout() != 0)
                ? app.getTimeout() : channel.getTimeout();

        // 失败处理策略：Application.failureStrategy 非 null 时透传；app 为 null 或策略为 null 时回退默认 FAIL_RETRY
        FailureStrategy strategy = (app != null && app.getFailureStrategy() != null)
                ? app.getFailureStrategy()
                : FailureStrategy.FAIL_RETRY;

        // 判断是否需要协议适配
        boolean needsAdaptation = endpoint.getProtocol() != protocol;

        // 组装 RoutingContext
        return new RoutingContext(
                channel.getId(),
                endpoint.getId(),
                endpoint.getEndpointUrl(),
                endpoint.getProtocol(),
                apiKey,
                effectiveTimeout,
                needsAdaptation,
                model.getModelName(),
                instance.getUpstreamModelName(),
                strategy
        );
    }
}
