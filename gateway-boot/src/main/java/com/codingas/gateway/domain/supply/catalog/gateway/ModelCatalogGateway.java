package com.codingas.gateway.domain.supply.catalog.gateway;

import com.codingas.gateway.domain.supply.catalog.entity.ModelCatalog;
import com.codingas.gateway.domain.supply.catalog.enums.CatalogSource;

import java.util.List;
import java.util.Optional;

/**
 * 模型目录网关接口
 *
 * <p>定义在 domain 层，由 infrastructure 层实现。</p>
 */
public interface ModelCatalogGateway {

    /**
     * 按唯一键 modelName 查找
     */
    Optional<ModelCatalog> findByModelName(String modelName);

    /**
     * 是否存在指定 modelName
     */
    boolean existsByModelName(String modelName);

    /**
     * 按数据来源查找所有条目
     */
    List<ModelCatalog> findBySource(CatalogSource source);

    /**
     * 查询所有
     */
    List<ModelCatalog> findAll();

    /**
     * 关键词搜索（modelName 或 displayName）
     */
    List<ModelCatalog> findByKeyword(String keyword);

    /**
     * 按能力过滤
     */
    List<ModelCatalog> findByCapability(String capability);

    /**
     * 保存
     */
    ModelCatalog save(ModelCatalog catalog);

    /**
     * 按来源查找，排除指定 modelName 集合
     *
     * <p>用于 markDeprecated：查找指定来源中不在 activeModelNames 集合里的条目。</p>
     */
    List<ModelCatalog> findBySourceExcludingKeys(CatalogSource source, List<String> activeModelNames);
}