package com.codingas.gateway.application.proxy.routing;

import com.codingas.gateway.application.resilience.ResilienceResolver;
import com.codingas.gateway.common.exception.ResourceNotFoundException;
import com.codingas.gateway.domain.resilience.entity.ResilienceProfile;
import com.codingas.gateway.domain.supply.entity.ModelInstance;
import com.codingas.gateway.domain.supply.enums.Protocol;
import com.codingas.gateway.domain.supply.enums.RoutingStrategy;
import com.codingas.gateway.domain.supply.gateway.ModelInstanceGateway;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 模型实例选择器 — 委托给 RouterChain 执行权限过滤 + 健康过滤 + 优先级分组，返回候选列表
 *
 * <p>Task 4.9：解析应用容灾画像（{@link ResilienceResolver}）并贯穿至 {@link RoutingRequest}，
 * 供 PinnedModelRouter/ClusterAffinityRouter/Invoker 链做画像化决策。画像解析失败（应用不存在、
 * default 画像缺失等）时降级为 null profile，不阻断路由（fail-open），仅在 DEBUG 日志记录。</p>
 */
@Component
@RequiredArgsConstructor
public class InstanceSelector {

    private static final Logger log = LoggerFactory.getLogger(InstanceSelector.class);

    private final ModelInstanceGateway modelInstanceGateway;
    private final RouterChain routerChain;
    /** 容灾画像解析器（Task 4.9 贯穿） */
    private final ResilienceResolver resilienceResolver;

    /**
     * 根据 modelId 和用户身份选择模型实例候选列表
     *
     * <p>返回按 priority 升序的候选列表（顺序由 {@link PriorityRouter} 保证），
     * 供 L1 故障转移逐个尝试。LoadBalanceRouter 已降级为透传，不再收敛到单实例。</p>
     *
     * @param modelId       模型 ID
     * @param applicationId 应用 ID（权限锚点；透传至 RoutingRequest 供 PermissionRouter 判定可见渠道）
     * @param userId        用户 ID
     * @param role          用户角色
     * @param strategy      路由策略
     * @param protocol      入站协议（透传至 RoutingRequest 供 HealthRouter 派生 endpointId）
     * @return 按 priority 升序的候选实例列表（顺序由 PriorityRouter 保证，供 L1 故障转移逐个尝试）
     * @throws ResourceNotFoundException 无可用实例
     */
    public List<ModelInstance> select(Long modelId, Long applicationId, Long userId, String role,
                                      RoutingStrategy strategy, Protocol protocol) {
        // 获取所有活跃实例（按 priority 升序）
        List<ModelInstance> allInstances = modelInstanceGateway.findActiveByModelIdOrderByPriority(modelId);
        if (allInstances.isEmpty()) {
            throw new ResourceNotFoundException("ModelInstance", modelId);
        }

        // 解析容灾画像贯穿路由链（fail-open：解析异常降级 null profile，不阻断路由）
        ResilienceProfile profile = resolveProfileSafely(applicationId);

        // 委托 RouterChain 执行过滤链（applicationId 权限锚点、protocol 派生 endpointId、profile 画像化决策）
        RoutingRequest request = new RoutingRequest(modelId, applicationId, userId, role, strategy, protocol, profile);
        List<ModelInstance> result = routerChain.filter(allInstances, request);

        if (result.isEmpty()) {
            throw new ResourceNotFoundException("ModelInstance", modelId);
        }

        // 返回候选列表（已按 priority 升序，供 L1 故障转移逐个尝试；不再收敛到单实例）
        return result;
    }

    /**
     * 解析容灾画像（fail-open）
     *
     * <p>applicationId 为 null 或画像解析抛异常时返回 null，避免画像解析失败阻断主路由链。
     * 画像缺失仅影响画像化决策（锁定/门禁），不影响基础路由。</p>
     *
     * @param applicationId 应用 ID
     * @return 容灾画像；解析失败或无应用 ID 时返回 null
     */
    private ResilienceProfile resolveProfileSafely(Long applicationId) {
        if (applicationId == null) {
            return null;
        }
        try {
            return resilienceResolver.resolve(applicationId);
        } catch (Exception e) {
            log.debug("容灾画像解析失败，降级为 null profile（fail-open）: applicationId={}, reason={}",
                    applicationId, e.getMessage());
            return null;
        }
    }
}
