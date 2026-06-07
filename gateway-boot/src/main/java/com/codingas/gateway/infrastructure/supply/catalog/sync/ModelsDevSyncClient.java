package com.codingas.gateway.infrastructure.supply.catalog.sync;

import com.codingas.gateway.domain.supply.catalog.entity.PlanCatalog;
import com.codingas.gateway.domain.supply.catalog.entity.PlanModelCatalog;
import com.codingas.gateway.domain.supply.entity.Model;
import com.codingas.gateway.domain.supply.entity.Provider;
import com.codingas.gateway.domain.supply.enums.BillingMode;
import com.codingas.gateway.domain.supply.enums.ProviderState;
import com.codingas.gateway.domain.supply.catalog.enums.CatalogState;
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
import java.util.Map;

/**
 * Models.dev 目录同步客户端
 *
 * <p>从 classpath:catalog/ JSON 文件加载目录数据并写入目录表。</p>
 * <p>后续可替换为真实的 Models.dev API 调用，只需替换 {@link #fetchProviders()}, {@link #fetchModels()},
 * {@link #fetchPlans()}, {@link #fetchPlanModels()} 四个方法的实现。</p>
 *
 * <p>同步流程：
 * <ol>
 *   <li>拉取四类目录数据</li>
 *   <li>逐条 upsert（由 {@link CatalogDomainService} 处理）</li>
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
            // Step 1: 同步供应商
            List<Provider> providers = fetchProviders();
            for (var provider : providers) {
                provider.setState(ProviderState.ACTIVE);
                String r = catalogDomainService.upsertProvider(provider);
                switch (r) {
                    case "ADDED" -> c.addedProviders++;
                    case "UPDATED" -> c.updatedProviders++;
                    default -> c.skippedProviders++;
                }
            }

            // Step 2: 同步模型规格目录
            List<Model> models = fetchModels();
            for (var model : models) {
                String r = catalogDomainService.upsertModel(model);
                switch (r) {
                    case "ADDED" -> c.addedModels++;
                    case "UPDATED" -> c.updatedModels++;
                    default -> c.skippedModels++;
                }
            }

            // Step 3: 同步套餐目录
            List<PlanCatalog> plans = fetchPlans();
            for (var catalog : plans) {
                catalog.setState(CatalogState.ACTIVE);
                catalog.setSyncedAt(now);
                String r = catalogDomainService.upsertPlan(catalog);
                switch (r) {
                    case "ADDED" -> c.addedPlans++;
                    case "UPDATED" -> c.updatedPlans++;
                    default -> c.skippedPlans++;
                }
            }

            // Step 4: 同步套餐模型关联
            List<PlanModelCatalog> planModels = fetchPlanModels();
            for (var catalog : planModels) {
                catalog.setState(CatalogState.ACTIVE);
                catalog.setSyncedAt(now);
                String r = catalogDomainService.upsertPlanModel(catalog);
                switch (r) {
                    case "ADDED" -> c.addedPlanModels++;
                    case "UPDATED" -> c.updatedPlanModels++;
                    default -> c.skippedPlanModels++;
                }
            }

            long elapsed = System.currentTimeMillis() - startTime;
            log.info("Models.dev 同步完成，耗时 {}ms: {}", elapsed, c.toResult());

        } catch (Exception e) {
            log.error("Models.dev 同步失败", e);
            throw new RuntimeException("Models.dev 同步失败", e);
        }

        return c.toResult();
    }

    /**
     * 拉取供应商数据
     *
     * <p>当前从 classpath JSON 文件加载，后续可替换为 Models.dev API 调用。</p>
     */
    protected List<Provider> fetchProviders() throws Exception {
        var dataList = loadJson("catalog/providers.json",
                new TypeReference<List<ProviderData>>() {});
        return dataList.stream().map(d -> {
            var p = new Provider();
            p.setCode(d.providerCode());
            p.setName(d.providerName());
            p.setLogoUrl(d.logoUrl());
            p.setWebsiteUrl(d.websiteUrl());
            p.setDescription(d.description());
            p.setPriority(100);
            return p;
        }).toList();
    }

    /**
     * 拉取模型目录数据
     *
     * <p>直接创建 Model 实体（替代原 ModelCatalog）。</p>
     * <p>capabilities 和 modalities 从 JSON 解析为 Map/List。</p>
     */
    protected List<Model> fetchModels() throws Exception {
        var dataList = loadJson("catalog/model-specs.json",
                new TypeReference<List<ModelData>>() {});
        return dataList.stream().map(d -> {
            var m = new Model();
            m.setModelName(d.modelName());
            m.setDisplayName(d.displayName());
            m.setModelFamily(d.modelFamily());
            m.setContextWindow(d.contextWindow());
            m.setMaxInputTokens(d.maxInputTokens());
            m.setMaxOutputTokens(d.maxOutputTokens());
            m.setKnowledgeCutoff(d.knowledgeCutoff());
            m.setCapabilities(parseCapabilities(d.capabilities()));
            m.setModalities(parseModalities(d.modalities()));
            return m;
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

    /**
     * 解析 capabilities JSON 对象为 Map
     */
    private Map<String, Boolean> parseCapabilities(Object capabilities) {
        if (capabilities == null) {
            return Map.of();
        }
        try {
            return syncObjectMapper.convertValue(capabilities, new TypeReference<Map<String, Boolean>>() {});
        } catch (Exception e) {
            log.warn("解析 capabilities 失败: {}", capabilities, e);
            return Map.of();
        }
    }

    /**
     * 解析 modalities JSON 对象为 List
     */
    private List<String> parseModalities(Object modalities) {
        if (modalities == null) {
            return List.of();
        }
        try {
            return syncObjectMapper.convertValue(modalities, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            log.warn("解析 modalities 失败: {}", modalities, e);
            return List.of();
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

    record ProviderData(String providerCode, String providerName,
                        String logoUrl, String websiteUrl, String description) {}

    record ModelData(String modelName, String displayName, String modelFamily,
                         Integer contextWindow, Integer maxInputTokens, Integer maxOutputTokens,
                         String knowledgeCutoff, Object capabilities, Object modalities) {}

    record PlanData(String planCode, String providerCode, String planName, String billingMode,
                    Object endpoints, Object pricing, String description) {}

    record PlanModelData(String planCode, String modelName) {}
}