package com.codingas.gateway.domain.resilience.gateway;

import com.codingas.gateway.domain.resilience.entity.Cluster;

import java.util.List;

/**
 * Cluster 故障域领域网关接口
 *
 * <p>故障域聚合根的持久化抽象。domain 层仅依赖此接口，
 * 实现位于 infrastructure 层（COLA Light 依赖倒置）。</p>
 */
public interface ClusterGateway {

    /**
     * 按主键查找故障域
     *
     * @param id 故障域 ID
     * @return 命中的故障域实体；不存在时返回 null
     */
    Cluster findById(Long id);

    /**
     * 按故障域编码查找
     *
     * @param code 故障域编码（全局唯一）
     * @return 命中的故障域实体；不存在时返回 null
     */
    Cluster findByCode(String code);

    /**
     * 查询全部故障域
     *
     * @return 故障域列表
     */
    List<Cluster> findAll();

    /**
     * 保存故障域
     *
     * @param cluster 故障域实体
     * @return 保存后的故障域实体（含生成的 ID 与审计字段）
     */
    Cluster save(Cluster cluster);
}
