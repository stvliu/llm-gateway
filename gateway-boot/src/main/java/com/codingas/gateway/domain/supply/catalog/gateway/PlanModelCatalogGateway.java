package com.codingas.gateway.domain.supply.catalog.gateway;

import com.codingas.gateway.domain.supply.catalog.entity.PlanModelCatalog;
import com.codingas.gateway.domain.supply.catalog.enums.CatalogSource;

import java.util.List;
import java.util.Optional;

/**
 * 套餐-模型关联目录网关接口
 *
 * <p>定义在 domain 层，由 infrastructure 层实现。</p>
 */
public interface PlanModelCatalogGateway {

    /**
     * 按唯一键 (planCode, modelName) 查找
     */
    Optional<PlanModelCatalog> findByPlanCodeAndModelName(String planCode, String modelName);

    /**
     * 按套餐查找
     */
    List<PlanModelCatalog> findByPlanCode(String planCode);

    /**
     * 按模型名查找
     */
    List<PlanModelCatalog> findByModelName(String modelName);

    /**
     * 按数据来源查找所有条目
     */
    List<PlanModelCatalog> findBySource(CatalogSource source);

    /**
     * 查询所有
     */
    List<PlanModelCatalog> findAll();

    /**
     * 保存
     */
    PlanModelCatalog save(PlanModelCatalog catalog);

    /**
     * 按来源查找，排除指定组合键集合
     *
     * <p>用于 markDeprecated：查找指定来源中不在活跃组合键集合里的条目。</p>
     * <p>activePlanCodes 和 activeProviderModelIds 构成笛卡尔排除条件——
     * 即排除 planCode 在 activePlanCodes 且 providerModelId 在 activeProviderModelIds 的记录。</p>
     */
    List<PlanModelCatalog> findBySourceExcludingKeys(CatalogSource source,
                                                     List<String> activePlanCodes,
                                                     List<String> activeModelNames);
}
