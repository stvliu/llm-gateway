package com.codingas.gateway.application.proxy.routing;

import com.codingas.gateway.domain.supply.entity.ModelInstance;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 加权随机负载均衡
 *
 * <p>参照 Dubbo RandomLoadBalance 实现：</p>
 * <ul>
 *   <li>所有权重相同 → ThreadLocalRandom.current().nextInt(n)</li>
 *   <li>权重不同 → 前缀和数组 + nextInt(totalWeight) 二分查找</li>
 * </ul>
 */
@Component("weightedRandomLoadBalance")
public class WeightedRandomLoadBalance extends AbstractLoadBalance {

    @Override
    protected ModelInstance doSelect(List<ModelInstance> instances) {
        int totalWeight = 0;
        boolean sameWeight = true;
        int firstWeight = getWeight(instances.getFirst());

        for (int i = 0; i < instances.size(); i++) {
            int weight = getWeight(instances.get(i));
            totalWeight += weight;
            if (sameWeight && i > 0 && weight != firstWeight) {
                sameWeight = false;
            }
        }

        if (totalWeight <= 0) {
            return instances.get(ThreadLocalRandom.current().nextInt(instances.size()));
        }

        if (!sameWeight) {
            // 加权随机：前缀和 + 二分查找
            int offset = ThreadLocalRandom.current().nextInt(totalWeight);
            for (ModelInstance instance : instances) {
                offset -= getWeight(instance);
                if (offset < 0) {
                    return instance;
                }
            }
        }

        // 所有权重相同，均匀随机
        return instances.get(ThreadLocalRandom.current().nextInt(instances.size()));
    }

    private int getWeight(ModelInstance instance) {
        return instance.getWeight() != null ? instance.getWeight() : 100;
    }
}
