package com.codingas.gateway.application.proxy.routing;

import com.codingas.gateway.domain.supply.entity.ModelInstance;
import java.util.List;

/**
 * 负载均衡策略接口
 *
 * <p>从候选实例列表中按策略选择一个实例。</p>
 */
@FunctionalInterface
public interface LoadBalance {

    /**
     * 从候选实例列表中选择一个
     *
     * @param instances 候选实例列表（非空）
     * @return 选中的实例
     */
    ModelInstance select(List<ModelInstance> instances);
}
