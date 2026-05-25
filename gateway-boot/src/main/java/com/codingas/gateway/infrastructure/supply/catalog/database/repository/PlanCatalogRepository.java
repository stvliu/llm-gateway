package com.codingas.gateway.infrastructure.supply.catalog.database.repository;

import com.codingas.gateway.infrastructure.supply.catalog.database.dataobject.PlanCatalogDo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 套餐目录 Repository
 */
public interface PlanCatalogRepository extends JpaRepository<PlanCatalogDo, Long> {

    Optional<PlanCatalogDo> findByPlanCode(String planCode);

    boolean existsByPlanCode(String planCode);

    List<PlanCatalogDo> findByProviderCode(String providerCode);

    List<PlanCatalogDo> findBySource(String source);
}
