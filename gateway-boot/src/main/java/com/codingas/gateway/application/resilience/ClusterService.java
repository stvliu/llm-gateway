package com.codingas.gateway.application.resilience;

import com.codingas.gateway.application.resilience.dto.ClusterRequest;
import com.codingas.gateway.application.resilience.dto.ClusterResponse;

import java.util.List;

/**
 * 故障域应用服务接口
 *
 * <p>管理 Cluster 故障域聚合根的 create/update/get/list。
 * 委托 {@link com.codingas.gateway.domain.resilience.gateway.ClusterGateway}。</p>
 *
 * <p>不提供 delete：Cluster 关联 Channel 的 clusterId，删除需级联清理；
 * 且 {@code ClusterGateway} 无 delete 方法，遵循既有模式不新增。</p>
 */
public interface ClusterService {

    /**
     * 创建故障域
     *
     * @param request 创建请求
     * @return 创建后的故障域响应
     */
    ClusterResponse create(ClusterRequest request);

    /**
     * 更新故障域
     *
     * @param id      故障域 ID
     * @param request 更新请求
     * @return 更新后的故障域响应
     */
    ClusterResponse update(Long id, ClusterRequest request);

    /**
     * 按主键查询故障域
     *
     * @param id 故障域 ID
     * @return 故障域响应
     */
    ClusterResponse getById(Long id);

    /**
     * 查询全部故障域
     *
     * @return 故障域响应列表
     */
    List<ClusterResponse> getAll();
}
