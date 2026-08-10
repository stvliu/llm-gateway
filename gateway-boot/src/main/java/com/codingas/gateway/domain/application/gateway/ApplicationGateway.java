/*
 * Copyright (c) 2025 codingas.com
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE file for details.
 */
package com.codingas.gateway.domain.application.gateway;

import com.codingas.gateway.domain.application.entity.Application;

import java.util.List;

/**
 * 应用领域网关接口
 *
 * <p>应用聚合根的持久化抽象。domain 层仅依赖此接口，
 * 实现位于 infrastructure 层（COLA Light 依赖倒置）。</p>
 */
public interface ApplicationGateway {

    /**
     * 按主键查找应用
     *
     * @param id 应用 ID
     * @return 命中的应用实体；不存在时返回 null
     */
    Application findById(Long id);

    /**
     * 按应用编码查找
     *
     * @param code 应用编码（全局唯一）
     * @return 命中的应用实体；不存在时返回 null
     */
    Application findByCode(String code);

    /**
     * 查询全部应用
     *
     * @return 应用列表
     */
    List<Application> findAll();

    /**
     * 保存应用
     *
     * @param app 应用实体
     * @return 保存后的应用实体（含生成的 ID 与审计字段）
     */
    Application save(Application app);

    /**
     * 删除应用
     *
     * @param id 应用 ID
     */
    void deleteById(Long id);
}
