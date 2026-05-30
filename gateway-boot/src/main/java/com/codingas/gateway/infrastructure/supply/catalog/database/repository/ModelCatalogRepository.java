package com.codingas.gateway.infrastructure.supply.catalog.database.repository;

import com.codingas.gateway.infrastructure.supply.catalog.database.dataobject.ModelCatalogDo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * 模型目录 Repository
 */
public interface ModelCatalogRepository extends JpaRepository<ModelCatalogDo, Long> {

    Optional<ModelCatalogDo> findByModelName(String modelName);

    boolean existsByModelName(String modelName);

    List<ModelCatalogDo> findBySource(String source);

    List<ModelCatalogDo> findByModelNameContainingOrDisplayNameContaining(String idKeyword, String nameKeyword);

    /**
     * 按能力过滤：使用 LIKE 查询 capabilities JSON 字段
     */
    @Query("SELECT m FROM ModelCatalogDo m WHERE m.capabilities LIKE %:capability%")
    List<ModelCatalogDo> findByCapability(@Param("capability") String capability);
}