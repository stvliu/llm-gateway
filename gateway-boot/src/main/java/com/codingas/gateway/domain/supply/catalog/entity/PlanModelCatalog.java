/*
 * Copyright (c) 2025 codingas.com
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE file for details.
 */
package com.codingas.gateway.domain.supply.catalog.entity;

import com.codingas.gateway.common.entity.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 套餐模型关联目录实体
 *
 * <p>纯关联行，表示套餐与模型规格的关联关系，不含定价字段。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class PlanModelCatalog extends BaseEntity {

    /** 套餐代码 → PlanCatalog */
    private String planCode;

    /** 供应商模型标识 → ModelCatalog */
    private String modelName;

    /** 目录状态，默认 ACTIVE */
    
}
