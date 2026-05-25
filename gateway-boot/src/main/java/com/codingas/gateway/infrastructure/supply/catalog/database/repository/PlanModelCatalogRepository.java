package com.codingas.gateway.infrastructure.supply.catalog.database.repository;

import com.codingas.gateway.infrastructure.supply.catalog.database.dataobject.PlanModelCatalogDo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 套餐-模型关联目录 Repository
 */
public interface PlanModelCatalogRepository extends JpaRepository<PlanModelCatalogDo, Long> {

    Optional<PlanModelCatalogDo> findByPlanCodeAndProviderModelId(String planCode, String providerModelId);

    List<PlanModelCatalogDo> findByPlanCode(String planCode);

    List<PlanModelCatalogDo> findByProviderModelId(String providerModelId);

    List<PlanModelCatalogDo> findBySource(String source);
}
