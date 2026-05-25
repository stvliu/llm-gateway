package com.codingas.gateway.infrastructure.supply.catalog.database.repository;

import com.codingas.gateway.infrastructure.supply.catalog.database.dataobject.ModelSpecCatalogDo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * 模型规格目录 Repository
 */
public interface ModelSpecCatalogRepository extends JpaRepository<ModelSpecCatalogDo, Long> {

    Optional<ModelSpecCatalogDo> findByProviderModelId(String providerModelId);

    boolean existsByProviderModelId(String providerModelId);

    List<ModelSpecCatalogDo> findBySource(String source);

    List<ModelSpecCatalogDo> findByProviderModelIdContainingOrDisplayNameContaining(String idKeyword, String nameKeyword);

    /**
     * 按能力过滤：使用 LIKE 查询 capabilities JSON 字段
     */
    @Query("SELECT m FROM ModelSpecCatalogDo m WHERE m.capabilities LIKE %:capability%")
    List<ModelSpecCatalogDo> findByCapability(@Param("capability") String capability);
}
