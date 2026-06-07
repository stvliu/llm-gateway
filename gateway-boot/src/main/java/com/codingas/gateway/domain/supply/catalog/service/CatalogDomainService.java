package com.codingas.gateway.domain.supply.catalog.service;

import com.codingas.gateway.domain.supply.catalog.entity.ModelCatalog;
import com.codingas.gateway.domain.supply.catalog.entity.PlanCatalog;
import com.codingas.gateway.domain.supply.catalog.entity.PlanModelCatalog;
import com.codingas.gateway.domain.supply.catalog.entity.ProviderCatalog;
import com.codingas.gateway.domain.supply.catalog.gateway.ModelCatalogGateway;
import com.codingas.gateway.domain.supply.catalog.gateway.PlanCatalogGateway;
import com.codingas.gateway.domain.supply.catalog.gateway.PlanModelCatalogGateway;
import com.codingas.gateway.domain.supply.catalog.gateway.ProviderCatalogGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 目录领域服务
 *
 * <p>封装 upsert 核心逻辑：按唯一键查找已有记录，若不存在则新增，若存在则更新字段。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CatalogDomainService {

    private final ProviderCatalogGateway providerCatalogGateway;
    private final PlanCatalogGateway planCatalogGateway;
    private final PlanModelCatalogGateway planModelCatalogGateway;
    private final ModelCatalogGateway modelCatalogGateway;

    // ===== upsert =====

    /**
     * 新增或更新供应商目录
     *
     * @param catalog 待写入的供应商目录
     * @return "ADDED" | "UPDATED"
     */
    @Transactional
    public String upsertProvider(ProviderCatalog catalog) {
        return providerCatalogGateway.findByProviderCode(catalog.getProviderCode())
                .map(existing -> {
                    copyProviderFields(catalog, existing);
                    providerCatalogGateway.save(existing);
                    return "UPDATED";
                })
                .orElseGet(() -> {
                    providerCatalogGateway.save(catalog);
                    return "ADDED";
                });
    }

    /**
     * 新增或更新套餐目录
     *
     * @param catalog 待写入的套餐目录
     * @return "ADDED" | "UPDATED"
     */
    @Transactional
    public String upsertPlan(PlanCatalog catalog) {
        return planCatalogGateway.findByPlanCode(catalog.getPlanCode())
                .map(existing -> {
                    copyPlanFields(catalog, existing);
                    planCatalogGateway.save(existing);
                    return "UPDATED";
                })
                .orElseGet(() -> {
                    planCatalogGateway.save(catalog);
                    return "ADDED";
                });
    }

    /**
     * 新增或更新套餐模型关联目录
     *
     * @param catalog 待写入的套餐模型关联目录
     * @return "ADDED" | "UPDATED"
     */
    @Transactional
    public String upsertPlanModel(PlanModelCatalog catalog) {
        return planModelCatalogGateway.findByPlanCodeAndModelName(
                        catalog.getPlanCode(), catalog.getModelName())
                .map(existing -> {
                    existing.setSyncedAt(catalog.getSyncedAt());
                    existing.setState(catalog.getState());
                    planModelCatalogGateway.save(existing);
                    return "UPDATED";
                })
                .orElseGet(() -> {
                    planModelCatalogGateway.save(catalog);
                    return "ADDED";
                });
    }

    /**
     * 新增或更新模型目录
     *
     * @param catalog 待写入的模型目录
     * @return "ADDED" | "UPDATED"
     */
    @Transactional
    public String upsertModel(ModelCatalog catalog) {
        return modelCatalogGateway.findByModelName(catalog.getModelName())
                .map(existing -> {
                    copyModelFields(catalog, existing);
                    modelCatalogGateway.save(existing);
                    return "UPDATED";
                })
                .orElseGet(() -> {
                    modelCatalogGateway.save(catalog);
                    return "ADDED";
                });
    }

    // ===== 字段拷贝 =====

    /**
     * 将源供应商目录的业务字段拷贝到目标实体
     */
    private void copyProviderFields(ProviderCatalog src, ProviderCatalog dst) {
        dst.setProviderName(src.getProviderName());
        dst.setLogoUrl(src.getLogoUrl());
        dst.setWebsiteUrl(src.getWebsiteUrl());
        dst.setDescription(src.getDescription());
        dst.setSyncedAt(src.getSyncedAt());
    }

    /**
     * 将源套餐目录的业务字段拷贝到目标实体
     */
    private void copyPlanFields(PlanCatalog src, PlanCatalog dst) {
        dst.setPlanName(src.getPlanName());
        dst.setBillingMode(src.getBillingMode());
        dst.setEndpoints(src.getEndpoints());
        dst.setPricing(src.getPricing());
        dst.setDescription(src.getDescription());
        dst.setSyncedAt(src.getSyncedAt());
    }

    /**
     * 将源模型目录的业务字段拷贝到目标实体
     */
    private void copyModelFields(ModelCatalog src, ModelCatalog dst) {
        dst.setDisplayName(src.getDisplayName());
        dst.setModelFamily(src.getModelFamily());
        dst.setContextWindow(src.getContextWindow());
        dst.setMaxInputTokens(src.getMaxInputTokens());
        dst.setMaxOutputTokens(src.getMaxOutputTokens());
        dst.setKnowledgeCutoff(src.getKnowledgeCutoff());
        dst.setCapabilities(src.getCapabilities());
        dst.setModalities(src.getModalities());
        dst.setSyncedAt(src.getSyncedAt());
    }
}