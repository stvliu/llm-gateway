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
package com.codingas.gateway.providerdata.model;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * 模型 Repository
 */
public interface ModelJpaRepository extends JpaRepository<ModelDo, Long> {

    Optional<ModelDo> findByModelName(String modelName);

    boolean existsByModelName(String modelName);

    List<ModelDo> findByIdIn(List<Long> ids);

    /**
     * 关键词搜索（modelName 或 displayName 包含关键字）
     */
    List<ModelDo> findByModelNameContainingOrDisplayNameContaining(String modelNameKeyword, String displayNameKeyword);

    /**
     * 按能力过滤：使用原生查询匹配 capabilities JSON 字段
     */
    @Query(value = "SELECT * FROM models WHERE CAST(capabilities AS text) LIKE CONCAT('%', :capability, '%')", nativeQuery = true)
    List<ModelDo> findByCapability(@Param("capability") String capability);
}