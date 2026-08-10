/*
 * Copyright (c) 2025 codingas.com
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE file for details.
 */
package com.codingas.gateway.infrastructure.supply.gateway.database.repository;

import com.codingas.gateway.infrastructure.supply.gateway.database.dataobject.ModelDo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * 模型 Repository
 */
public interface ModelRepository extends JpaRepository<ModelDo, Long> {

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