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
package com.codingas.gateway.providerdata.repository;

import com.codingas.gateway.providerdata.dataobject.PlanModelCatalogDo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 套餐-模型关联目录 Repository
 */
public interface PlanModelCatalogRepository extends JpaRepository<PlanModelCatalogDo, Long> {

    Optional<PlanModelCatalogDo> findByPlanCodeAndModelName(String planCode, String modelName);

    List<PlanModelCatalogDo> findByPlanCode(String planCode);

    List<PlanModelCatalogDo> findByModelName(String modelName);
}
