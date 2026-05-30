package com.codingas.gateway.infrastructure.supply.catalog.loader;

import com.codingas.gateway.domain.supply.catalog.entity.ModelCatalog;
import com.codingas.gateway.domain.supply.catalog.entity.PlanCatalog;
import com.codingas.gateway.domain.supply.catalog.entity.PlanModelCatalog;
import com.codingas.gateway.domain.supply.catalog.entity.ProviderCatalog;
import com.codingas.gateway.domain.supply.catalog.enums.BillingMode;
import com.codingas.gateway.domain.supply.catalog.enums.CatalogSource;
import com.codingas.gateway.domain.supply.catalog.enums.CatalogState;
import com.codingas.gateway.domain.supply.catalog.enums.ProviderType;
import com.codingas.gateway.domain.supply.catalog.gateway.PlanCatalogGateway;
import com.codingas.gateway.domain.supply.catalog.gateway.PlanModelCatalogGateway;
import com.codingas.gateway.domain.supply.catalog.gateway.ProviderCatalogGateway;
import com.codingas.gateway.domain.supply.catalog.service.CatalogDomainService;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.List;

/**
 * 内置目录数据加载器
 *
 * <p>应用启动时，如果 catalog 表为空，从 classpath:catalog/*.json 加载内置数据。</p>
 */
@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class BuiltinCatalogLoader implements CommandLineRunner {

    private final ProviderCatalogGateway providerCatalogGateway;
    private final PlanCatalogGateway planCatalogGateway;
    private final ObjectMapper objectMapper;
    private final CatalogDomainService catalogDomainService;

    /** 用于反序列化 JSON 文件的 ObjectMapper（忽略未知字段如 source/syncedAt/state） */
    private ObjectMapper catalogObjectMapper;

    @Override
    public void run(String... args) {
        loadIfNeeded();
    }

    /**
     * 如果表为空则加载 BUILTIN 数据（启动时自动调用）
     */
    public void loadIfNeeded() {
        if (!providerCatalogGateway.findAll().isEmpty()) {
            log.info("目录数据已存在，跳过 BUILTIN 加载");
            return;
        }
        doLoad();
    }

    /**
     * 强制重新加载 BUILTIN 数据（手动触发同步时调用）
     *
     * <p>不检查表是否为空，直接执行 upsert 操作。</p>
     * <p>upsert 规则保证已有记录不会重复创建，只会被更新。</p>
     */
    public void forceReload() {
        doLoad();
    }

    /**
     * 执行 BUILTIN 数据加载
     */
    private void doLoad() {
        // 延迟初始化：复制全局 ObjectMapper 并忽略未知字段
        // JSON 文件中含 source/syncedAt/state 等元数据字段，record 中不包含
        catalogObjectMapper = objectMapper.copy()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

        log.info("开始加载 BUILTIN 目录数据...");
        long startTime = System.currentTimeMillis();

        try {
            loadProviders();
            loadModels();
            loadPlans();
            loadPlanModels();

            long elapsed = System.currentTimeMillis() - startTime;
            log.info("BUILTIN 目录数据加载完成，耗时 {}ms", elapsed);
        } catch (Exception e) {
            log.error("BUILTIN 目录数据加载失败", e);
            // 不抛出异常，允许应用启动
        }
    }

    /**
     * 加载供应商目录
     */
    private void loadProviders() throws Exception {
        var providers = loadJson("catalog/providers.json", new TypeReference<List<ProviderCatalogData>>() {});
        int added = 0, updated = 0, skipped = 0;
        for (var data : providers) {
            var catalog = new ProviderCatalog();
            catalog.setProviderCode(data.providerCode());
            catalog.setProviderName(data.providerName());
            catalog.setProviderType(ProviderType.valueOf(data.providerType()));
            catalog.setLogoUrl(data.logoUrl());
            catalog.setWebsiteUrl(data.websiteUrl());
            catalog.setDescription(data.description());
            catalog.setSource(CatalogSource.BUILTIN);
            catalog.setState(CatalogState.ACTIVE);

            String result = catalogDomainService.upsertProvider(catalog);
            switch (result) {
                case "ADDED" -> added++;
                case "UPDATED" -> updated++;
                default -> skipped++;
            }
        }
        log.info("加载供应商目录: added={}, updated={}, skipped={}", added, updated, skipped);
    }

    /**
     * 加载模型目录
     */
    private void loadModels() throws Exception {
        var specs = loadJson("catalog/model-specs.json", new TypeReference<List<ModelCatalogData>>() {});
        int added = 0, updated = 0, skipped = 0;
        for (var data : specs) {
            var catalog = new ModelCatalog();
            catalog.setModelName(data.modelName());
            catalog.setDisplayName(data.displayName());
            catalog.setModelFamily(data.modelFamily());
            catalog.setContextWindow(data.contextWindow());
            catalog.setMaxInputTokens(data.maxInputTokens());
            catalog.setMaxOutputTokens(data.maxOutputTokens());
            catalog.setKnowledgeCutoff(data.knowledgeCutoff());
            // capabilities 和 modalities 存储为 JSON String
            catalog.setCapabilities(data.capabilities() != null ? objectMapper.writeValueAsString(data.capabilities()) : null);
            catalog.setModalities(data.modalities() != null ? objectMapper.writeValueAsString(data.modalities()) : null);
            catalog.setSource(CatalogSource.BUILTIN);
            catalog.setState(CatalogState.ACTIVE);

            String result = catalogDomainService.upsertModel(catalog);
            switch (result) {
                case "ADDED" -> added++;
                case "UPDATED" -> updated++;
                default -> skipped++;
            }
        }
        log.info("加载模型目录: added={}, updated={}, skipped={}", added, updated, skipped);
    }

    /**
     * 加载套餐目录
     */
    private void loadPlans() throws Exception {
        var plans = loadJson("catalog/plans.json", new TypeReference<List<PlanCatalogData>>() {});
        int added = 0, updated = 0, skipped = 0;
        for (var data : plans) {
            var catalog = new PlanCatalog();
            catalog.setPlanCode(data.planCode());
            catalog.setProviderCode(data.providerCode());
            catalog.setPlanName(data.planName());
            catalog.setBillingMode(BillingMode.valueOf(data.billingMode()));
            // endpoints 和 pricing 存储为 JSON String
            catalog.setEndpoints(data.endpoints() != null ? objectMapper.writeValueAsString(data.endpoints()) : null);
            catalog.setPricing(data.pricing() != null ? objectMapper.writeValueAsString(data.pricing()) : null);
            catalog.setDescription(data.description());
            catalog.setSource(CatalogSource.BUILTIN);
            catalog.setState(CatalogState.ACTIVE);

            String result = catalogDomainService.upsertPlan(catalog);
            switch (result) {
                case "ADDED" -> added++;
                case "UPDATED" -> updated++;
                default -> skipped++;
            }
        }
        log.info("加载套餐目录: added={}, updated={}, skipped={}", added, updated, skipped);
    }

    /**
     * 加载套餐模型关联目录
     */
    private void loadPlanModels() throws Exception {
        var planModels = loadJson("catalog/plan-models.json", new TypeReference<List<PlanModelCatalogData>>() {});
        int added = 0, updated = 0, skipped = 0;
        for (var data : planModels) {
            var catalog = new PlanModelCatalog();
            catalog.setPlanCode(data.planCode());
            catalog.setModelName(data.modelName());
            catalog.setSource(CatalogSource.BUILTIN);
            catalog.setState(CatalogState.ACTIVE);

            String result = catalogDomainService.upsertPlanModel(catalog);
            switch (result) {
                case "ADDED" -> added++;
                case "UPDATED" -> updated++;
                default -> skipped++;
            }
        }
        log.info("加载套餐模型关联目录: added={}, updated={}, skipped={}", added, updated, skipped);
    }

    /**
     * 从 classpath 读取 JSON 文件
     */
    private <T> List<T> loadJson(String path, TypeReference<List<T>> typeRef) throws Exception {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(path)) {
            if (is == null) {
                throw new IllegalStateException("未找到资源文件: " + path);
            }
            return catalogObjectMapper.readValue(is, typeRef);
        }
    }

    // ===== JSON 数据 record（用于反序列化） =====

    record ProviderCatalogData(
        String providerCode, String providerName, String providerType,
        String logoUrl, String websiteUrl, String description
    ) {}

    record ModelCatalogData(
        @JsonProperty("providerModelId") String modelName, String displayName, String modelFamily,
        Integer contextWindow, Integer maxInputTokens, Integer maxOutputTokens,
        String knowledgeCutoff, Object capabilities, Object modalities
    ) {}

    record PlanCatalogData(
        String planCode, String providerCode, String planName, String billingMode,
        Object endpoints, Object pricing, String description
    ) {}

    record PlanModelCatalogData(
        String planCode, @JsonProperty("providerModelId") String modelName
    ) {}
}