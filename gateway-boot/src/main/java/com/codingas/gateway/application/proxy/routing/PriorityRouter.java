package com.codingas.gateway.application.proxy.routing;

import com.codingas.gateway.domain.supply.entity.ModelInstance;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

/**
 * 优先级路由器 — 按 priority 升序排序器，输出完整候选列表不收敛
 *
 * <p>顺序语义：排在 {@link HealthRouter} 之后（@Order 300 > 200），
 * 仅在健康候选（已剔除熔断渠道）中按优先级升序排序，输出全部候选，
 * 供 L1 故障转移逐个尝试，避免收敛到最小组导致备渠道丢失。</p>
 *
 * <p>排序键：{@link ModelInstance#getPriority()}，null 回退默认值 100（数值越小越优先）。
 * 注：当前按实例级 priority 排序，应用级渠道 priority 映射将在后续 Task 切换。</p>
 */
@Component
@Order(300)
public class PriorityRouter implements Router {

    /** priority 为 null 时的回退默认值（与实体默认值一致） */
    private static final int DEFAULT_PRIORITY = 100;

    @Override
    public List<ModelInstance> filter(List<ModelInstance> instances, RoutingRequest request) {
        if (instances.isEmpty()) {
            return List.of();
        }

        // 按 priority 升序输出完整列表（不收敛到最小组），数值越小越优先；null 回退默认值 100
        return instances.stream()
                .sorted(Comparator.comparingInt(this::effectivePriority))
                .toList();
    }

    /**
     * 解析有效 priority：null 回退默认值 100
     *
     * @param mi 模型实例
     * @return 有效 priority
     */
    private int effectivePriority(ModelInstance mi) {
        return mi.getPriority() != null ? mi.getPriority() : DEFAULT_PRIORITY;
    }

    @Override
    public boolean isForce() { return true; }
}
