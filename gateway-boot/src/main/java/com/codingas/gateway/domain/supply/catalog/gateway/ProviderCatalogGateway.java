package com.codingas.gateway.domain.supply.catalog.gateway;

import com.codingas.gateway.domain.supply.catalog.entity.ProviderCatalog;

import java.util.List;
import java.util.Optional;

/**
 * 供应商目录持久化接口
 */
public interface ProviderCatalogGateway {

    ProviderCatalog save(ProviderCatalog catalog);

    Optional<ProviderCatalog> findByProviderCode(String providerCode);

    List<ProviderCatalog> findAll();

    void deleteByProviderCode(String providerCode);
}