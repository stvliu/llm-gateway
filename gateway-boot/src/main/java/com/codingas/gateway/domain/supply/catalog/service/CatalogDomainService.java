package com.codingas.gateway.domain.supply.catalog.service;

import com.codingas.gateway.domain.supply.catalog.entity.PlanCatalog;
import com.codingas.gateway.domain.supply.catalog.entity.PlanModelCatalog;
import com.codingas.gateway.domain.supply.catalog.gateway.PlanCatalogGateway;
import com.codingas.gateway.domain.supply.catalog.gateway.PlanModelCatalogGateway;
import com.codingas.gateway.domain.supply.entity.Model;
import com.codingas.gateway.domain.supply.entity.Provider;
import com.codingas.gateway.domain.supply.enums.ModelState;
import com.codingas.gateway.domain.supply.enums.ProviderState;
import com.codingas.gateway.domain.supply.gateway.ModelGateway;
import com.codingas.gateway.domain.supply.gateway.ProviderGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * 目录领域服务
 *
 * <p>封装 upsert 核心逻辑：按唯一键查找已有记录，若不存在则新增，若存在则更新字段。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CatalogDomainService {

    private final ProviderGateway providerGateway;
    private final PlanCatalogGateway planCatalogGateway;
    private final PlanModelCatalogGateway planModelCatalogGateway;
    private final ModelGateway modelGateway;

    // ===== upsert =====

    /**
     * 新增或更新供应商
     *
     * @param provider 待写入的供应商
     * @return "ADDED" | "UPDATED"
     */
    @Transactional
    public String upsertProvider(Provider provider) {
        return providerGateway.findByCode(provider.getCode())
                .map(existing -> {
                    copyProviderFields(provider, existing);
                    providerGateway.save(existing);
                    return "UPDATED";
                })
                .orElseGet(() -> {
                    provider.setState(ProviderState.ACTIVE);
                    providerGateway.save(provider);
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
     * 新增或更新模型
     *
     * <p>直接操作 Model 实体（替代原 ModelCatalog）。</p>
     * <p>capabilities 和 modalities 已从 JSON 字符串解析为 Map/List。</p>
     *
     * @param model 待写入的模型
     * @return "ADDED" | "UPDATED"
     */
    @Transactional
    public String upsertModel(Model model) {
        return modelGateway.findByModelName(model.getModelName())
                .map(existing -> {
                    copyModelFields(model, existing);
                    modelGateway.save(existing);
                    return "UPDATED";
                })
                .orElseGet(() -> {
                    model.setState(ModelState.ACTIVE);
                    modelGateway.save(model);
                    return "ADDED";
                });
    }

    // ===== 字段拷贝 =====

    /**
     * 将源供应商的业务字段拷贝到目标实体
     */
    private void copyProviderFields(Provider src, Provider dst) {
        dst.setName(src.getName());
        dst.setLogoUrl(src.getLogoUrl());
        dst.setWebsiteUrl(src.getWebsiteUrl());
        dst.setDescription(src.getDescription());
        dst.setApiDocUrl(src.getApiDocUrl());
        dst.setPriority(src.getPriority());
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
     * 将源模型的业务字段拷贝到目标实体
     */
    private void copyModelFields(Model src, Model dst) {
        dst.setDisplayName(src.getDisplayName());
        dst.setModelFamily(src.getModelFamily());
        dst.setContextWindow(src.getContextWindow());
        dst.setMaxInputTokens(src.getMaxInputTokens());
        dst.setMaxOutputTokens(src.getMaxOutputTokens());
        dst.setKnowledgeCutoff(src.getKnowledgeCutoff());
        dst.setCapabilities(src.getCapabilities());
        dst.setModalities(src.getModalities());
    }
}
