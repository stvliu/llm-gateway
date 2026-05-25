package com.codingas.gateway.infrastructure.supply.catalog.database.repository;

import com.codingas.gateway.infrastructure.supply.catalog.database.dataobject.ProviderCatalogDo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 供应商目录 Repository
 */
public interface ProviderCatalogRepository extends JpaRepository<ProviderCatalogDo, Long> {

    Optional<ProviderCatalogDo> findByProviderCode(String providerCode);

    boolean existsByProviderCode(String providerCode);

    List<ProviderCatalogDo> findBySource(String source);

    List<ProviderCatalogDo> findByProviderType(String providerType);

    List<ProviderCatalogDo> findByProviderCodeContainingOrProviderNameContaining(String codeKeyword, String nameKeyword);
}
