/*
 * Copyright (c) 2025 codingas.com
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE file for details.
 */
package com.codingas.gateway.domain.supply.gateway;

import com.codingas.gateway.domain.supply.entity.Model;

import java.util.List;
import java.util.Optional;

/**
 * 模型持久化接口
 *
 * <p>定义在 domain 层，由 infrastructure 层实现。</p>
 */
public interface ModelGateway {

    /**
     * 保存模型
     */
    Model save(Model model);

    /**
     * 根据 ID 查找模型
     */
    Optional<Model> findById(Long id);

    /**
     * 根据模型名查找模型
     */
    Optional<Model> findByModelName(String modelName);

    /**
     * 查找同名模型的所有活跃渠道
     */
    List<Model> findActiveByModelName(String modelName);

    /**
     * 查询所有模型
     */
    List<Model> findAll();

    /**
     * 查找所有活跃模型
     */
    List<Model> findAllActive();

    /**
     * 批量查找模型
     */
    List<Model> findByIds(List<Long> ids);

    /**
     * 统计模型总数
     */
    long count();

    /**
     * 删除模型
     */
    void delete(Model model);

    /**
     * 检查模型名是否存在
     */
    boolean existsByModelName(String modelName);

    /**
     * 关键词搜索（modelName 或 displayName 包含关键字）
     */
    List<Model> findByKeyword(String keyword);

    /**
     * 按能力过滤（capabilities 包含指定能力）
     */
    List<Model> findByCapability(String capability);

    /**
     * 获取最大版本号
     */
    default long getMaxVersion() {
        return 0L;
    }
}