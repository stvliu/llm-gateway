package com.codingas.gateway.application.proxy.routing;

import com.codingas.gateway.domain.supply.entity.ModelInstance;
import java.util.List;

/**
 * 负载均衡抽象基类
 *
 * <p>提供空检查和单元素短路。</p>
 */
public abstract class AbstractLoadBalance implements LoadBalance {

    @Override
    public ModelInstance select(List<ModelInstance> instances) {
        if (instances == null || instances.isEmpty()) {
            return null;
        }
        if (instances.size() == 1) {
            return instances.getFirst();
        }
        return doSelect(instances);
    }

    /**
     * 实际选择逻辑（由子类实现）
     */
    protected abstract ModelInstance doSelect(List<ModelInstance> instances);
}
