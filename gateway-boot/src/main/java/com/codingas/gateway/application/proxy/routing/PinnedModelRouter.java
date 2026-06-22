package com.codingas.gateway.application.proxy.routing;

import com.codingas.gateway.domain.resilience.entity.ResilienceProfile;
import com.codingas.gateway.domain.supply.entity.ModelInstance;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 模型锁定路由器 — 按画像 pinnedModelId 锁定候选模型
 *
 * <p>顺序语义：排在 {@link PriorityRouter}（@Order 300）之后（@Order 350）。
 * 在健康过滤、优先级择优、域亲和之后，若画像启用模型锁定，仅保留 pinnedModelId
 * 对应的实例，强制流量固定到指定模型渠道（容灾方案设计.md strict 档位 PINNED 语义）。</p>
 *
 * <p>锁定规则（从 {@link RoutingRequest#getResilienceProfile()} 取画像）：</p>
 * <ul>
 *   <li>画像为 null 或 {@code enablePinnedModel=false}：透传全部候选（不锁定）</li>
 *   <li>{@code enablePinnedModel=true} 但 {@code pinnedModelId=null}：透传全部（无锁定目标）</li>
 *   <li>{@code enablePinnedModel=true} 且 {@code pinnedModelId} 非空：仅保留
 *       {@code modelId == pinnedModelId} 的实例；候选无匹配则返回空</li>
 * </ul>
 *
 * <p>{@link #isForce()} 返回 false：锁定后候选为空时让链继续而非终止，
 * 保留后续 Router 兜底（虽 @Order 350 后通常仅剩 LoadBalanceRouter 透传）。</p>
 */
@Component
@Order(350)
public class PinnedModelRouter implements Router {

    @Override
    public List<ModelInstance> filter(List<ModelInstance> instances, RoutingRequest request) {
        if (instances.isEmpty()) {
            return List.of();
        }

        ResilienceProfile profile = request.getResilienceProfile();
        if (profile == null || !profile.isEnablePinnedModel()) {
            // 无画像或未启用锁定：透传
            return instances;
        }

        Long pinnedModelId = profile.getPinnedModelId();
        if (pinnedModelId == null) {
            // 启用锁定但未指定目标：透传（配置不完整，不强制清空）
            return instances;
        }

        // 仅保留锁定模型对应的实例
        return instances.stream()
                .filter(mi -> pinnedModelId.equals(mi.getModelId()))
                .toList();
    }

    @Override
    public boolean isForce() {
        // 锁定后空则让链继续，保留兜底
        return false;
    }
}
