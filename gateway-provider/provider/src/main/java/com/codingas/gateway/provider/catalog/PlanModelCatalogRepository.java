/*
 * Copyright © 2025-2026 codingas.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.codingas.gateway.provider.catalog;

import com.codingas.gateway.provider.catalog.PlanModelCatalog;

import java.util.List;
import java.util.Optional;

/**
 * 套餐-模型关联目录网关接口
 *
 * <p>定义在 domain 层，由 infrastructure 层实现。</p>
 */
public interface PlanModelCatalogRepository {

    /**
     * 按唯一键 (planCode, modelName) 查找
     */
    Optional<PlanModelCatalog> findByPlanCodeAndModelName(String planCode, String modelName);

    /**
     * 按模型名查找
     */
    List<PlanModelCatalog> findByModelName(String modelName);

    /**
     * 查询所有
     */
    List<PlanModelCatalog> findAll();

    /**
     * 保存
     */
    PlanModelCatalog save(PlanModelCatalog catalog);
}
