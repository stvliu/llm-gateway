package com.codingas.gateway.core.domain.gateway;

import com.codingas.gateway.core.domain.entity.RouteGroup;

import java.util.List;
import java.util.Optional;

/**
 * 路由分组网关接口
 *
 * <p>定义在 domain 层，由 infrastructure 层实现。</p>
 * <p>Domain 不直接依赖持久化，通过此接口操作路由分组。</p>
 */
public interface RouteGroupGateway {

    /**
     * 根据分组编码查找路由分组
     *
     * @param groupCode 分组编码
     * @return 路由分组，不存在返回空
     */
    Optional<RouteGroup> findByGroupCode(String groupCode);

    /**
     * 根据分组 ID 查找路由分组
     *
     * @param groupId 分组 ID
     * @return 路由分组，不存在返回空
     */
    Optional<RouteGroup> findById(Long groupId);

    /**
     * 查找所有活跃路由分组
     *
     * @return 活跃路由分组列表
     */
    List<RouteGroup> findAllActive();

    /**
     * 保存路由分组
     *
     * @param routeGroup 路由分组实体
     * @return 保存后的实体
     */
    RouteGroup save(RouteGroup routeGroup);
}
