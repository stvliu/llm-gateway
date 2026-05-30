package com.codingas.gateway.domain.supply.catalog.service;

import com.codingas.gateway.domain.supply.catalog.entity.ModelCatalog;
import com.codingas.gateway.domain.supply.catalog.entity.PlanCatalog;
import com.codingas.gateway.domain.supply.catalog.entity.PlanModelCatalog;
import com.codingas.gateway.domain.supply.catalog.entity.ProviderCatalog;
import com.codingas.gateway.domain.supply.catalog.enums.CatalogSource;
import com.codingas.gateway.domain.supply.catalog.enums.CatalogState;
import com.codingas.gateway.domain.supply.catalog.gateway.ModelCatalogGateway;
import com.codingas.gateway.domain.supply.catalog.gateway.PlanCatalogGateway;
import com.codingas.gateway.domain.supply.catalog.gateway.PlanModelCatalogGateway;
import com.codingas.gateway.domain.supply.catalog.gateway.ProviderCatalogGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 目录领域服务
 *
 * <p>封装 upsert（含 source 覆盖策略）、markDeprecated 核心逻辑。</p>
 *
 * <p>upsert 规则：按唯一键查找已有记录，若不存在则新增（ADDED），
 * 若存在且新来源优先级 >= 已有来源优先级则覆盖更新（UPDATED），否则跳过（SKIPPED）。</p>
 *
 * <p>markDeprecated 规则：查找指定来源中不在活跃键集合里的条目，
 * 将其状态从 ACTIVE 改为 DEPRECATED。</p>
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
     * @return "ADDED" | "UPDATED" | "SKIPPED"
     */
    @Transactional
    public String upsertProvider(ProviderCatalog catalog) {
        return providerCatalogGateway.findByProviderCode(catalog.getProviderCode())
                .map(existing -> {
                    if (catalog.getSource().canOverride(existing.getSource())) {
                        copyProviderFields(catalog, existing);
                        providerCatalogGateway.save(existing);
                        return "UPDATED";
                    }
                    return "SKIPPED";
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
     * @return "ADDED" | "UPDATED" | "SKIPPED"
     */
    @Transactional
    public String upsertPlan(PlanCatalog catalog) {
        return planCatalogGateway.findByPlanCode(catalog.getPlanCode())
                .map(existing -> {
                    if (catalog.getSource().canOverride(existing.getSource())) {
                        copyPlanFields(catalog, existing);
                        planCatalogGateway.save(existing);
                        return "UPDATED";
                    }
                    return "SKIPPED";
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
     * @return "ADDED" | "UPDATED" | "SKIPPED"
     */
    @Transactional
    public String upsertPlanModel(PlanModelCatalog catalog) {
        return planModelCatalogGateway.findByPlanCodeAndModelName(
                        catalog.getPlanCode(), catalog.getModelName())
                .map(existing -> {
                    if (catalog.getSource().canOverride(existing.getSource())) {
                        existing.setSource(catalog.getSource());
                        existing.setSyncedAt(catalog.getSyncedAt());
                        existing.setState(catalog.getState());
                        planModelCatalogGateway.save(existing);
                        return "UPDATED";
                    }
                    return "SKIPPED";
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
     * @return "ADDED" | "UPDATED" | "SKIPPED"
     */
    @Transactional
    public String upsertModel(ModelCatalog catalog) {
        return modelCatalogGateway.findByModelName(catalog.getModelName())
                .map(existing -> {
                    if (catalog.getSource().canOverride(existing.getSource())) {
                        copyModelFields(catalog, existing);
                        modelCatalogGateway.save(existing);
                        return "UPDATED";
                    }
                    return "SKIPPED";
                })
                .orElseGet(() -> {
                    modelCatalogGateway.save(catalog);
                    return "ADDED";
                });
    }

    // ===== markDeprecated =====

    /**
     * 将指定来源中不在活跃集合里的供应商目录标记为 DEPRECATED
     *
     * @param source      数据来源
     * @param activeCodes 仍然活跃的 providerCode 集合
     */
    @Transactional
    public void markProvidersDeprecated(CatalogSource source, List<String> activeCodes) {
        var toDeprecate = providerCatalogGateway.findBySourceExcludingKeys(source, activeCodes);
        for (var entry : toDeprecate) {
            if (entry.getState() == CatalogState.ACTIVE) {
                entry.setState(CatalogState.DEPRECATED);
                providerCatalogGateway.save(entry);
                log.info("Deprecated provider: code={}", entry.getProviderCode());
            }
        }
    }

    /**
     * 将指定来源中不在活跃集合里的套餐目录标记为 DEPRECATED
     *
     * @param source         数据来源
     * @param activePlanCodes 仍然活跃的 planCode 集合
     */
    @Transactional
    public void markPlansDeprecated(CatalogSource source, List<String> activePlanCodes) {
        var toDeprecate = planCatalogGateway.findBySourceExcludingKeys(source, activePlanCodes);
        for (var entry : toDeprecate) {
            if (entry.getState() == CatalogState.ACTIVE) {
                entry.setState(CatalogState.DEPRECATED);
                planCatalogGateway.save(entry);
                log.info("Deprecated plan: code={}", entry.getPlanCode());
            }
        }
    }

    /**
     * 将指定来源中不在活跃集合里的模型目录标记为 DEPRECATED
     *
     * @param source         数据来源
     * @param activeModelNames 仍然活跃的 modelName 集合
     */
    @Transactional
    public void markModelsDeprecated(CatalogSource source, List<String> activeModelNames) {
        var toDeprecate = modelCatalogGateway.findBySourceExcludingKeys(source, activeModelNames);
        for (var entry : toDeprecate) {
            if (entry.getState() == CatalogState.ACTIVE) {
                entry.setState(CatalogState.DEPRECATED);
                modelCatalogGateway.save(entry);
                log.info("Deprecated model: name={}", entry.getModelName());
            }
        }
    }

    /**
     * 将指定来源中不在活跃集合里的套餐模型关联目录标记为 DEPRECATED
     *
     * @param source         数据来源
     * @param activePlanCodes 仍然活跃的 planCode 集合
     * @param activeModelNames  仍然活跃的 modelName 集合
     */
    @Transactional
    public void markPlanModelsDeprecated(CatalogSource source,
                                         List<String> activePlanCodes,
                                         List<String> activeModelNames) {
        var toDeprecate = planModelCatalogGateway.findBySourceExcludingKeys(source, activePlanCodes, activeModelNames);
        for (var entry : toDeprecate) {
            if (entry.getState() == CatalogState.ACTIVE) {
                entry.setState(CatalogState.DEPRECATED);
                planModelCatalogGateway.save(entry);
                log.info("Deprecated plan-model: plan={}, model={}", entry.getPlanCode(), entry.getModelName());
            }
        }
    }

    // ===== 字段拷贝 =====

    /**
     * 将源供应商目录的业务字段拷贝到目标实体
     */
    private void copyProviderFields(ProviderCatalog src, ProviderCatalog dst) {
        dst.setProviderName(src.getProviderName());
        dst.setProviderType(src.getProviderType());
        dst.setLogoUrl(src.getLogoUrl());
        dst.setWebsiteUrl(src.getWebsiteUrl());
        dst.setDescription(src.getDescription());
        dst.setSource(src.getSource());
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
        dst.setSource(src.getSource());
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
        dst.setSource(src.getSource());
        dst.setSyncedAt(src.getSyncedAt());
    }
}