package com.codingas.gateway.application.proxy.routing;

import com.codingas.gateway.domain.supply.entity.ModelInstance;
import java.util.List;

/**
 * 路由器接口 — 对候选实例列表执行过滤，返回符合条件的子集
 */
@FunctionalInterface
public interface Router {

    List<ModelInstance> filter(List<ModelInstance> instances, RoutingRequest request);

    default boolean isForce() { return false; }
}
