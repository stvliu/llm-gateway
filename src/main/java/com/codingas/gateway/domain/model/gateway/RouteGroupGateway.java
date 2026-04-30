package com.codingas.gateway.domain.model.gateway;

import com.codingas.gateway.domain.model.entity.RouteGroup;

import java.util.List;

/**
 * 路由分组网关接口
 *
 * <p>定义在 domain 层，由 infrastructure 层实现。</p>
 */
public interface RouteGroupGateway {

    /**
     * 根据 ID 查找路由分组
     *
     * @param id 分组 ID
     * @return 分组信息，不存在返回 null
     */
    RouteGroup findById(Long id);

    /**
     * 根据分组代码查找路由分组
     *
     * @param groupCode 分组代码
     * @return 分组信息，不存在返回 null
     */
    RouteGroup findByGroupCode(String groupCode);

    /**
     * 查找所有活跃路由分组
     *
     * @return 活跃分组列表
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
