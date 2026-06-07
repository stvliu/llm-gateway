package com.codingas.gateway.domain.supply.catalog.gateway;

import com.codingas.gateway.domain.supply.catalog.entity.PlanModelCatalog;

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
     * 查询所有
     */
    List<PlanModelCatalog> findAll();

    /**
     * 保存
     */
    PlanModelCatalog save(PlanModelCatalog catalog);
}
