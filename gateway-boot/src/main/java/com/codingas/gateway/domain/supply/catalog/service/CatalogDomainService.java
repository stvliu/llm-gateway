package com.codingas.gateway.domain.supply.catalog.service;

import com.codingas.gateway.domain.supply.catalog.entity.ModelCatalog;
import com.codingas.gateway.domain.supply.catalog.entity.ProviderCatalog;
import com.codingas.gateway.domain.supply.catalog.gateway.ModelCatalogGateway;
import com.codingas.gateway.domain.supply.catalog.gateway.ProviderCatalogGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 目录领域服务
 *
 * <p>封装目录相关的核心业务逻辑。</p>
 */
@Service
@RequiredArgsConstructor
public class CatalogDomainService {

    private final ProviderCatalogGateway providerCatalogGateway;
    private final ModelCatalogGateway modelCatalogGateway;

    /**
     * 保存供应商目录
     */
    public ProviderCatalog saveProviderCatalog(ProviderCatalog catalog) {
        return providerCatalogGateway.save(catalog);
    }

    /**
     * 查找所有供应商目录
     */
    public List<ProviderCatalog> findAllProviderCatalogs() {
        return providerCatalogGateway.findAll();
    }

    /**
     * 根据供应商代码查找目录
     */
    public ProviderCatalog findProviderCatalogByCode(String providerCode) {
        return providerCatalogGateway.findByProviderCode(providerCode).orElse(null);
    }

    /**
     * 保存模型目录
     */
    public ModelCatalog saveModelCatalog(ModelCatalog catalog) {
        return modelCatalogGateway.save(catalog);
    }

    /**
     * 查找所有模型目录
     */
    public List<ModelCatalog> findAllModelCatalogs() {
        return modelCatalogGateway.findAll();
    }

    /**
     * 根据供应商代码查找模型目录
     */
    public List<ModelCatalog> findModelCatalogsByProviderCode(String providerCode) {
        return modelCatalogGateway.findByProviderCode(providerCode);
    }
}