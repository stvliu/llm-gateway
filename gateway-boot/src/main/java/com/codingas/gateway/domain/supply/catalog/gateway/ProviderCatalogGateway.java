package com.codingas.gateway.domain.supply.catalog.gateway;

import com.codingas.gateway.domain.supply.catalog.entity.ProviderCatalog;
import com.codingas.gateway.domain.supply.catalog.enums.CatalogSource;
import com.codingas.gateway.domain.supply.catalog.enums.ProviderType;

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
     * 按数据来源查找所有条目
     */
    List<ProviderCatalog> findBySource(CatalogSource source);

    /**
     * 按供应商类型查找
     */
    List<ProviderCatalog> findByProviderType(ProviderType providerType);

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

    /**
     * 按来源查找，排除指定 providerCode 集合
     *
     * <p>用于 markDeprecated：查找指定来源中不在 activeProviderCodes 集合里的条目。</p>
     */
    List<ProviderCatalog> findBySourceExcludingKeys(CatalogSource source, List<String> activeProviderCodes);
}
