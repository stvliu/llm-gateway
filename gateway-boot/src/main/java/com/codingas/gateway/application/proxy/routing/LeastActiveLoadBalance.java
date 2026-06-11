package com.codingas.gateway.application.proxy.routing;

import com.codingas.gateway.domain.supply.entity.ModelInstance;
import com.codingas.gateway.infrastructure.resilience.EndpointMetricsRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 最少活跃负载均衡
 *
 * <p>参照 Dubbo LeastActiveLoadBalance：</p>
 * <ul>
 *   <li>通过 EndpointMetricsRegistry 获取活跃数</li>
 *   <li>选活跃数最少的实例</li>
 *   <li>同活跃度内按 weight 加权随机</li>
 * </ul>
 */
@Component("leastActiveLoadBalance")
@RequiredArgsConstructor
public class LeastActiveLoadBalance extends AbstractLoadBalance {

    private final EndpointMetricsRegistry metricsRegistry;

    @Override
    protected ModelInstance doSelect(List<ModelInstance> instances) {
        int leastActive = Integer.MAX_VALUE;
        int leastCount = 0;
        int[] leastIndexes = new int[instances.size()];
        int totalWeight = 0;
        int firstWeight = 0;
        boolean sameWeight = true;

        for (int i = 0; i < instances.size(); i++) {
            ModelInstance instance = instances.get(i);
            int active = metricsRegistry.get(instance.getChannelId()).getActive();

            if (active < leastActive) {
                // 发现更小的活跃数，重置
                leastActive = active;
                leastCount = 1;
                leastIndexes[0] = i;
                totalWeight = getWeight(instance);
                firstWeight = totalWeight;
                sameWeight = true;
            } else if (active == leastActive) {
                // 同活跃度，累加权重
                leastIndexes[leastCount++] = i;
                totalWeight += getWeight(instance);
                if (sameWeight && totalWeight != firstWeight * leastCount) {
                    sameWeight = false;
                }
            }
        }

        if (leastCount == 1) {
            return instances.get(leastIndexes[0]);
        }

        // 同活跃度内加权随机
        if (!sameWeight && totalWeight > 0) {
            int offset = ThreadLocalRandom.current().nextInt(totalWeight);
            for (int i = 0; i < leastCount; i++) {
                offset -= getWeight(instances.get(leastIndexes[i]));
                if (offset < 0) {
                    return instances.get(leastIndexes[i]);
                }
            }
        }

        // 同权重，均匀随机
        return instances.get(leastIndexes[ThreadLocalRandom.current().nextInt(leastCount)]);
    }

    private int getWeight(ModelInstance instance) {
        return instance.getWeight() != null ? instance.getWeight() : 100;
    }
}
