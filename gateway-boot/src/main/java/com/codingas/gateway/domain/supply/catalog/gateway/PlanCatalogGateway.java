/*
 * Copyright (c) 2025 codingas.com
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE file for details.
 */
package com.codingas.gateway.domain.supply.catalog.gateway;

import com.codingas.gateway.domain.supply.catalog.entity.PlanCatalog;

import java.util.List;
import java.util.Optional;

/**
 * 套餐目录网关接口
 *
 * <p>定义在 domain 层，由 infrastructure 层实现。</p>
 */
public interface PlanCatalogGateway {

    /**
     * 按唯一键 planCode 查找
     */
    Optional<PlanCatalog> findByPlanCode(String planCode);

    /**
     * 是否存在指定 planCode
     */
    boolean existsByPlanCode(String planCode);

    /**
     * 按供应商查找
     */
    List<PlanCatalog> findByProviderCode(String providerCode);

    /**
     * 查询所有
     */
    List<PlanCatalog> findAll();

    /**
     * 保存
     */
    PlanCatalog save(PlanCatalog catalog);
}
