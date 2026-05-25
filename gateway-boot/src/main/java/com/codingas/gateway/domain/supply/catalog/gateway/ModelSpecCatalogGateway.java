package com.codingas.gateway.domain.supply.catalog.gateway;

import com.codingas.gateway.domain.supply.catalog.entity.ModelSpecCatalog;
import com.codingas.gateway.domain.supply.catalog.enums.CatalogSource;

import java.util.List;
import java.util.Optional;

/**
 * 模型规格目录网关接口
 *
 * <p>定义在 domain 层，由 infrastructure 层实现。</p>
 */
public interface ModelSpecCatalogGateway {

    /**
     * 按唯一键 providerModelId 查找
     */
    Optional<ModelSpecCatalog> findByProviderModelId(String providerModelId);

    /**
     * 是否存在指定 providerModelId
     */
    boolean existsByProviderModelId(String providerModelId);

    /**
     * 按数据来源查找所有条目
     */
    List<ModelSpecCatalog> findBySource(CatalogSource source);

    /**
     * 查询所有
     */
    List<ModelSpecCatalog> findAll();

    /**
     * 关键词搜索（providerModelId 或 displayName）
     */
    List<ModelSpecCatalog> findByKeyword(String keyword);

    /**
     * 按能力过滤
     */
    List<ModelSpecCatalog> findByCapability(String capability);

    /**
     * 保存
     */
    ModelSpecCatalog save(ModelSpecCatalog catalog);

    /**
     * 按来源查找，排除指定 providerModelId 集合
     *
     * <p>用于 markDeprecated：查找指定来源中不在 activeProviderModelIds 集合里的条目。</p>
     */
    List<ModelSpecCatalog> findBySourceExcludingKeys(CatalogSource source, List<String> activeProviderModelIds);
}
