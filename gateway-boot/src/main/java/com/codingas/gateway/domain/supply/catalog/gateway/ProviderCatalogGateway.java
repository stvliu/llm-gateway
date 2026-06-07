package com.codingas.gateway.domain.supply.catalog.gateway;

import com.codingas.gateway.domain.supply.catalog.entity.ProviderCatalog;

import java.util.List;
import java.util.Optional;

/**
 * 供应商目录网关接口
 *
 * <p>定义在 domain 层，由 infrastructure 层实现。</p>
 */
public interface ProviderCatalogGateway {

    /**
     * 按唯一键 providerCode 查找
     */
    Optional<ProviderCatalog> findByProviderCode(String providerCode);

    /**
     * 是否存在指定 providerCode
     */
    boolean existsByProviderCode(String providerCode);

    /**
     * 查询所有
     */
    List<ProviderCatalog> findAll();

    /**
     * 关键词搜索（providerCode 或 providerName）
     */
    List<ProviderCatalog> findByKeyword(String keyword);

    /**
     * 保存
     */
    ProviderCatalog save(ProviderCatalog catalog);
}
