package com.codingas.gateway.domain.resilience.gateway;

import com.codingas.gateway.domain.resilience.entity.ResilienceProfile;

import java.util.List;

/**
 * 容灾画像领域网关接口
 *
 * <p>容灾画像聚合根的持久化抽象。domain 层仅依赖此接口，
 * 实现位于 infrastructure 层（COLA Light 依赖倒置）。</p>
 */
public interface ResilienceProfileGateway {

    /**
     * 按主键查找容灾画像
     *
     * @param id 画像 ID
     * @return 命中的画像实体；不存在时返回 null
     */
    ResilienceProfile findById(Long id);

    /**
     * 按画像编码查找
     *
     * @param code 画像编码（全局唯一）
     * @return 命中的画像实体；不存在时返回 null
     */
    ResilienceProfile findByCode(String code);

    /**
     * 查询全部容灾画像
     *
     * @return 画像列表
     */
    List<ResilienceProfile> findAll();

    /**
     * 保存容灾画像
     *
     * @param profile 画像实体
     * @return 保存后的画像实体（含生成的 ID 与审计字段）
     */
    ResilienceProfile save(ResilienceProfile profile);
}
