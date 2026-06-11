package com.codingas.gateway.application.proxy.routing;

import com.codingas.gateway.domain.supply.entity.ModelInstance;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 平滑加权轮询负载均衡
 *
 * <p>参照 Dubbo RoundRobinLoadBalance 的平滑加权轮询算法：</p>
 * <ul>
 *   <li>每个实例维护 current 值，初始化 0</li>
 *   <li>选择：current += weight，选 current 最大的</li>
 *   <li>选中：current -= totalWeight</li>
 * </ul>
 */
@Component("roundRobinLoadBalance")
public class RoundRobinLoadBalance extends AbstractLoadBalance {

    private final ConcurrentMap<Integer, AtomicInteger> currents = new ConcurrentHashMap<>();
    private static final int RECYCLE_PERIOD = 60000; // 60 秒

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

        if (!sameWeight) {
            return doRoundRobin(instances, totalWeight);
        }

        // 所有权重相同，简单轮询
        int idx = getCurrent(0).updateAndGet(c -> (c + 1) % instances.size());
        return instances.get(Math.abs(idx) % instances.size());
    }

    private ModelInstance doRoundRobin(List<ModelInstance> instances, int totalWeight) {
        int maxCurrent = Integer.MIN_VALUE;
        int selectedIndex = 0;

        for (int i = 0; i < instances.size(); i++) {
            int weight = getWeight(instances.get(i));
            int current = getCurrent(i).addAndGet(weight);
            if (current > maxCurrent) {
                maxCurrent = current;
                selectedIndex = i;
            }
        }

        getCurrent(selectedIndex).addAndGet(-totalWeight);
        return instances.get(selectedIndex);
    }

    private AtomicInteger getCurrent(int index) {
        return currents.computeIfAbsent(index, k -> new AtomicInteger(0));
    }

    private int getWeight(ModelInstance instance) {
        return instance.getWeight() != null ? instance.getWeight() : 100;
    }
}
