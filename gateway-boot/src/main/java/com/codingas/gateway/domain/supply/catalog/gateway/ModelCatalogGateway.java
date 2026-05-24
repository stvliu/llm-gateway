package com.codingas.gateway.domain.supply.catalog.gateway;

import com.codingas.gateway.domain.supply.catalog.entity.ModelCatalog;

import java.util.List;
import java.util.Optional;

/**
 * 模型目录持久化接口
 */
public interface ModelCatalogGateway {

    ModelCatalog save(ModelCatalog catalog);

    Optional<ModelCatalog> findByProviderModelId(String providerModelId);

    List<ModelCatalog> findAll();

    List<ModelCatalog> findByProviderCode(String providerCode);

    void deleteByProviderModelId(String providerModelId);
}