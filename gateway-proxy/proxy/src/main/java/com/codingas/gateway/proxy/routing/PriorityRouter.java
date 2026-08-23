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
package com.codingas.gateway.proxy.routing;

import com.codingas.gateway.provider.model.ModelInstance;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * 优先级路由器 — 按应用级 priority 升序排序器，输出完整候选列表不收敛
 *
 * <p>顺序语义：排在 {@link HealthRouter} 之后（@Order 300 > 200），
 * 仅在健康候选（已剔除熔断渠道）中按优先级升序排序，输出全部候选，
 * 供 L1 故障转移逐个尝试，避免收敛到最小组导致备渠道丢失。</p>
 *
 * <p>排序键（Task 3）：{@link RoutingRequest#getChannelPriorityMap()} 中
 * {@code map.get(mi.getChannelId())}，null 回退默认值 100（数值越小越优先）。
 * 同一渠道在不同应用可有不同 priority，实现应用级转移顺序；映射为空时全部回退 100。</p>
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

        // 取应用级渠道优先级映射；按 map.get(channelId) 升序输出完整列表（不收敛），null 回退默认值 100
        Map<Long, Integer> channelPriorityMap = request.getChannelPriorityMap();
        return instances.stream()
                .sorted(Comparator.comparingInt(mi -> effectivePriority(mi, channelPriorityMap)))
                .toList();
    }

    /**
     * 解析有效 priority：从应用级映射按 channelId 取值，null 回退默认值 100
     *
     * <p>channelId 为 null 时直接回退默认值（不可变 Map 不允许 null key，避免 NPE）。</p>
     *
     * @param mi                 模型实例
     * @param channelPriorityMap 应用级渠道优先级映射（key=channelId, value=priority）
     * @return 有效 priority
     */
    private int effectivePriority(ModelInstance mi, Map<Long, Integer> channelPriorityMap) {
        Long channelId = mi.getChannelId();
        if (channelId == null) {
            return DEFAULT_PRIORITY;
        }
        Integer priority = channelPriorityMap.get(channelId);
        return priority != null ? priority : DEFAULT_PRIORITY;
    }

    @Override
    public boolean isForce() { return true; }
}
