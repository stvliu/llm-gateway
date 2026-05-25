package com.codingas.gateway.domain.supply.catalog.gateway;

import com.codingas.gateway.domain.supply.catalog.entity.PlanCatalog;
import com.codingas.gateway.domain.supply.catalog.enums.CatalogSource;

import java.util.List;
import java.util.Optional;

/**
 * 套餐目录网关接口
 *
 * <p>定义在 domain 层，由 infrastructure 层实现。</p>
 */
public interface PlanCatalogGateway {

    /**
     * 按唯一键 planCode 查找
     */
    Optional<PlanCatalog> findByPlanCode(String planCode);

    /**
     * 是否存在指定 planCode
     */
    boolean existsByPlanCode(String planCode);

    /**
     * 按供应商查找
     */
    List<PlanCatalog> findByProviderCode(String providerCode);

    /**
     * 按数据来源查找所有条目
     */
    List<PlanCatalog> findBySource(CatalogSource source);

    /**
     * 查询所有
     */
    List<PlanCatalog> findAll();

    /**
     * 保存
     */
    PlanCatalog save(PlanCatalog catalog);

    /**
     * 按来源查找，排除指定 planCode 集合
     *
     * <p>用于 markDeprecated：查找指定来源中不在 activePlanCodes 集合里的条目。</p>
     */
    List<PlanCatalog> findBySourceExcludingKeys(CatalogSource source, List<String> activePlanCodes);
}
