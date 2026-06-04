package com.codingas.gateway.infrastructure.supply.catalog.sync;

import com.codingas.gateway.domain.supply.catalog.entity.ModelCatalog;
import com.codingas.gateway.domain.supply.catalog.entity.PlanCatalog;
import com.codingas.gateway.domain.supply.catalog.entity.PlanModelCatalog;
import com.codingas.gateway.domain.supply.catalog.entity.ProviderCatalog;
import com.codingas.gateway.domain.supply.enums.BillingMode;
import com.codingas.gateway.domain.supply.catalog.enums.CatalogSource;
import com.codingas.gateway.domain.supply.catalog.enums.CatalogState;
import com.codingas.gateway.domain.supply.catalog.enums.ProviderType;
import com.codingas.gateway.domain.supply.catalog.service.CatalogDomainService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Models.dev 目录同步客户端
 *
 * <p>从 classpath:catalog/ JSON 文件加载目录数据，以 {@link CatalogSource#MODELS_DEV} 来源写入目录表。
 * 后续可替换为真实的 Models.dev API 调用，只需替换 {@link #fetchProviders()}, {@link #fetchModels()},
 * {@link #fetchPlans()}, {@link #fetchPlanModels()} 四个方法的实现。</p>
 *
 * <p>同步流程：
 * <ol>
 *   <li>拉取四类目录数据</li>
 *   <li>逐条 upsert（由 {@link CatalogDomainService} 按 source 优先级处理覆盖策略）</li>
 *   <li>标记已消失的条目为 DEPRECATED</li>
 * </ol>
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ModelsDevSyncClient {

    private final ObjectMapper objectMapper;
    private final CatalogDomainService catalogDomainService;

    /** 用于反序列化 JSON 文件的 ObjectMapper（忽略未知字段） */
    private ObjectMapper syncObjectMapper;

    /**
     * 执行 Models.dev 全量同步
     *
     * @return 同步统计结果
     */
    public SyncResult sync() {
        syncObjectMapper = objectMapper.copy()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

        log.info("开始 Models.dev 同步...");
        long startTime = System.currentTimeMillis();

        Counters c = new Counters();
        Instant now = Instant.now();

        try {
            // Step 1: 同步供应商目录
            List<ProviderCatalog> providers = fetchProviders();
            List<String> providerCodes = new ArrayList<>();
            for (var catalog : providers) {
                catalog.setSource(CatalogSource.MODELS_DEV);
                catalog.setState(CatalogState.ACTIVE);
                catalog.setSyncedAt(now);
                String r = catalogDomainService.upsertProvider(catalog);
                switch (r) {
                    case "ADDED" -> c.addedProviders++;
                    case "UPDATED" -> c.updatedProviders++;
                    default -> c.skippedProviders++;
                }
                providerCodes.add(catalog.getProviderCode());
            }
            catalogDomainService.markProvidersDeprecated(CatalogSource.MODELS_DEV, providerCodes);

            // Step 2: 同步模型规格目录
            List<ModelCatalog> models = fetchModels();
            List<String> modelIds = new ArrayList<>();
            for (var catalog : models) {
                catalog.setSource(CatalogSource.MODELS_DEV);
                catalog.setState(CatalogState.ACTIVE);
                catalog.setSyncedAt(now);
                String r = catalogDomainService.upsertModel(catalog);
                switch (r) {
                    case "ADDED" -> c.addedModels++;
                    case "UPDATED" -> c.updatedModels++;
                    default -> c.skippedModels++;
                }
                modelIds.add(catalog.getModelName());
            }
            catalogDomainService.markModelsDeprecated(CatalogSource.MODELS_DEV, modelIds);

            // Step 3: 同步套餐目录
            List<PlanCatalog> plans = fetchPlans();
            List<String> planCodes = new ArrayList<>();
            for (var catalog : plans) {
                catalog.setSource(CatalogSource.MODELS_DEV);
                catalog.setState(CatalogState.ACTIVE);
                catalog.setSyncedAt(now);
                String r = catalogDomainService.upsertPlan(catalog);
                switch (r) {
                    case "ADDED" -> c.addedPlans++;
                    case "UPDATED" -> c.updatedPlans++;
                    default -> c.skippedPlans++;
                }
                planCodes.add(catalog.getPlanCode());
            }
            catalogDomainService.markPlansDeprecated(CatalogSource.MODELS_DEV, planCodes);

            // Step 4: 同步套餐模型关联
            List<PlanModelCatalog> planModels = fetchPlanModels();
            List<String> activePlanCodes = new ArrayList<>();
            List<String> activeModelIds = new ArrayList<>();
            for (var catalog : planModels) {
                catalog.setSource(CatalogSource.MODELS_DEV);
                catalog.setState(CatalogState.ACTIVE);
                catalog.setSyncedAt(now);
                String r = catalogDomainService.upsertPlanModel(catalog);
                switch (r) {
                    case "ADDED" -> c.addedPlanModels++;
                    case "UPDATED" -> c.updatedPlanModels++;
                    default -> c.skippedPlanModels++;
                }
                activePlanCodes.add(catalog.getPlanCode());
                activeModelIds.add(catalog.getModelName());
            }
            catalogDomainService.markPlanModelsDeprecated(
                    CatalogSource.MODELS_DEV, activePlanCodes, activeModelIds);

            long elapsed = System.currentTimeMillis() - startTime;
            log.info("Models.dev 同步完成，耗时 {}ms: {}", elapsed, c.toResult());

        } catch (Exception e) {
            log.error("Models.dev 同步失败", e);
            throw new RuntimeException("Models.dev 同步失败", e);
        }

        return c.toResult();
    }

    /**
     * 拉取供应商目录数据
     *
     * <p>当前从 classpath JSON 文件加载，后续可替换为 Models.dev API 调用。</p>
     */
    protected List<ProviderCatalog> fetchProviders() throws Exception {
        var dataList = loadJson("catalog/providers.json",
                new TypeReference<List<ProviderData>>() {});
        return dataList.stream().map(d -> {
            var c = new ProviderCatalog();
            c.setProviderCode(d.providerCode());
            c.setProviderName(d.providerName());
            c.setProviderType(ProviderType.valueOf(d.providerType()));
            c.setLogoUrl(d.logoUrl());
            c.setWebsiteUrl(d.websiteUrl());
            c.setDescription(d.description());
            return c;
        }).toList();
    }

    /**
     * 拉取模型目录数据
     */
    protected List<ModelCatalog> fetchModels() throws Exception {
        var dataList = loadJson("catalog/model-specs.json",
                new TypeReference<List<ModelData>>() {});
        return dataList.stream().map(d -> {
            var c = new ModelCatalog();
            c.setModelName(d.modelName());
            c.setDisplayName(d.displayName());
            c.setModelFamily(d.modelFamily());
            c.setContextWindow(d.contextWindow());
            c.setMaxInputTokens(d.maxInputTokens());
            c.setMaxOutputTokens(d.maxOutputTokens());
            c.setKnowledgeCutoff(d.knowledgeCutoff());
            c.setCapabilities(d.capabilities() != null ? toJson(d.capabilities()) : null);
            c.setModalities(d.modalities() != null ? toJson(d.modalities()) : null);
            return c;
        }).toList();
    }

    /**
     * 拉取套餐目录数据
     */
    protected List<PlanCatalog> fetchPlans() throws Exception {
        var dataList = loadJson("catalog/plans.json",
                new TypeReference<List<PlanData>>() {});
        return dataList.stream().map(d -> {
            var c = new PlanCatalog();
            c.setPlanCode(d.planCode());
            c.setProviderCode(d.providerCode());
            c.setPlanName(d.planName());
            c.setBillingMode(BillingMode.resolve(d.billingMode()));
            c.setEndpoints(d.endpoints() != null ? toJson(d.endpoints()) : null);
            c.setPricing(d.pricing() != null ? toJson(d.pricing()) : null);
            c.setDescription(d.description());
            return c;
        }).toList();
    }

    /**
     * 拉取套餐模型关联数据
     */
    protected List<PlanModelCatalog> fetchPlanModels() throws Exception {
        var dataList = loadJson("catalog/plan-models.json",
                new TypeReference<List<PlanModelData>>() {});
        return dataList.stream().map(d -> {
            var c = new PlanModelCatalog();
            c.setPlanCode(d.planCode());
            c.setModelName(d.modelName());
            return c;
        }).toList();
    }

    // ===== 辅助方法 =====

    private <T> List<T> loadJson(String path, TypeReference<List<T>> typeRef) throws Exception {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(path)) {
            if (is == null) {
                log.warn("未找到资源文件: {}, 返回空列表", path);
                return List.of();
            }
            return syncObjectMapper.readValue(is, typeRef);
        }
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            log.warn("JSON 序列化失败", e);
            return null;
        }
    }

    // ===== 同步统计结果 =====

    /**
     * 同步统计结果
     */
    public record SyncResult(
            int addedProviders, int updatedProviders, int skippedProviders,
            int addedModels, int updatedModels, int skippedModels,
            int addedPlans, int updatedPlans, int skippedPlans,
            int addedPlanModels, int updatedPlanModels, int skippedPlanModels
    ) {
        public int totalAdded() {
            return addedProviders + addedModels + addedPlans + addedPlanModels;
        }

        public int totalUpdated() {
            return updatedProviders + updatedModels + updatedPlans + updatedPlanModels;
        }
    }

    /** 可变计数器（用于累积同步过程中的统计值） */
    private static class Counters {
        int addedProviders, updatedProviders, skippedProviders;
        int addedModels, updatedModels, skippedModels;
        int addedPlans, updatedPlans, skippedPlans;
        int addedPlanModels, updatedPlanModels, skippedPlanModels;

        SyncResult toResult() {
            return new SyncResult(
                    addedProviders, updatedProviders, skippedProviders,
                    addedModels, updatedModels, skippedModels,
                    addedPlans, updatedPlans, skippedPlans,
                    addedPlanModels, updatedPlanModels, skippedPlanModels);
        }
    }

    // ===== JSON 数据 record（用于反序列化） =====

    record ProviderData(String providerCode, String providerName, String providerType,
                        String logoUrl, String websiteUrl, String description) {}

    record ModelData(String modelName, String displayName, String modelFamily,
                         Integer contextWindow, Integer maxInputTokens, Integer maxOutputTokens,
                         String knowledgeCutoff, Object capabilities, Object modalities) {}

    record PlanData(String planCode, String providerCode, String planName, String billingMode,
                    Object endpoints, Object pricing, String description) {}

    record PlanModelData(String planCode, String modelName) {}
}