package com.codingas.gateway.infrastructure.supply.catalog.loader;

import com.codingas.gateway.domain.supply.catalog.entity.PlanCatalog;
import com.codingas.gateway.domain.supply.catalog.entity.PlanModelCatalog;
import com.codingas.gateway.domain.supply.entity.Model;
import com.codingas.gateway.domain.supply.entity.Provider;
import com.codingas.gateway.domain.supply.enums.BillingMode;
import com.codingas.gateway.domain.supply.catalog.gateway.PlanCatalogGateway;
import com.codingas.gateway.domain.supply.catalog.gateway.PlanModelCatalogGateway;
import com.codingas.gateway.domain.supply.gateway.ModelGateway;
import com.codingas.gateway.domain.supply.gateway.ProviderGateway;
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
import java.util.Map;

/**
 * 内置数据加载器
 *
 * <p>应用启动时，如果核心表为空，从 classpath:catalog/*.json 加载内置数据。</p>
 * <p>负责加载 Provider、Model、PlanCatalog、PlanModelCatalog 四类数据。</p>
 * <p>从 BuiltinCatalogLoader 重构而来，适配新的供应域架构。</p>
 */
@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class BuiltinDataLoader implements CommandLineRunner {

    private final ProviderGateway providerGateway;
    private final PlanCatalogGateway planCatalogGateway;
    private final PlanModelCatalogGateway planModelCatalogGateway;
    private final ModelGateway modelGateway;
    private final ObjectMapper objectMapper;

    /** 用于反序列化 JSON 文件的 ObjectMapper（忽略未知字段如 syncedAt/state） */
    private ObjectMapper catalogObjectMapper;

    @Override
    public void run(String... args) {
        loadIfNeeded();
    }

    /**
     * 如果表为空则加载 BUILTIN 数据（启动时自动调用）
     */
    public void loadIfNeeded() {
        if (!providerGateway.findAll().isEmpty()) {
            log.info("内置数据已存在，跳过 BUILTIN 加载");
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
        // JSON 文件中含 syncedAt/state 等元数据字段，record 中不包含
        catalogObjectMapper = objectMapper.copy()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

        log.info("开始加载 BUILTIN 数据...");
        long startTime = System.currentTimeMillis();

        try {
            loadProviders();
            loadModels();
            loadPlans();
            loadPlanModels();

            long elapsed = System.currentTimeMillis() - startTime;
            log.info("BUILTIN 数据加载完成，耗时 {}ms", elapsed);
        } catch (Exception e) {
            log.error("BUILTIN 数据加载失败", e);
            // 不抛出异常，允许应用启动
        }
    }

    /**
     * 加载供应商数据
     */
    private void loadProviders() throws Exception {
        var providers = loadJson("catalog/providers.json", new TypeReference<List<ProviderCatalogData>>() {});
        int added = 0, updated = 0, skipped = 0;
        for (var data : providers) {
            var provider = new Provider();
            provider.setCode(data.providerCode());
            provider.setName(data.providerName());
            provider.setLogoUrl(data.logoUrl());
            provider.setWebsiteUrl(data.websiteUrl());
            provider.setDescription(data.description());
            provider.setPriority(100);

            String result = upsertProvider(provider);
            switch (result) {
                case "ADDED" -> added++;
                case "UPDATED" -> updated++;
                default -> skipped++;
            }
        }
        log.info("加载供应商: added={}, updated={}, skipped={}", added, updated, skipped);
    }

    /**
     * 加载模型数据
     *
     * <p>直接创建 Model 实体（替代原 ModelCatalog）。</p>
     * <p>capabilities 和 modalities 从 JSON 解析为 Map/List。</p>
     */
    private void loadModels() throws Exception {
        var specs = loadJson("catalog/model-specs.json", new TypeReference<List<ModelCatalogData>>() {});
        int added = 0, updated = 0, skipped = 0;
        for (var data : specs) {
            var model = new Model();
            model.setModelName(data.modelName());
            model.setDisplayName(data.displayName());
            model.setModelFamily(data.modelFamily());
            model.setContextWindow(data.contextWindow());
            model.setMaxInputTokens(data.maxInputTokens());
            model.setMaxOutputTokens(data.maxOutputTokens());
            model.setKnowledgeCutoff(data.knowledgeCutoff());
            // capabilities 和 modalities 解析为 Map/List
            model.setCapabilities(parseCapabilities(data.capabilities()));
            model.setModalities(parseModalities(data.modalities()));

            String result = upsertModel(model);
            switch (result) {
                case "ADDED" -> added++;
                case "UPDATED" -> updated++;
                default -> skipped++;
            }
        }
        log.info("加载模型: added={}, updated={}, skipped={}", added, updated, skipped);
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
            catalog.setBillingMode(BillingMode.resolve(data.billingMode()));
            // endpoints 和 pricing 存储为 JSON String
            catalog.setEndpoints(data.endpoints() != null ? objectMapper.writeValueAsString(data.endpoints()) : null);
            catalog.setPricing(data.pricing() != null ? objectMapper.writeValueAsString(data.pricing()) : null);
            catalog.setDescription(data.description());

            String result = upsertPlan(catalog);
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

            String result = upsertPlanModel(catalog);
            switch (result) {
                case "ADDED" -> added++;
                case "UPDATED" -> updated++;
                default -> skipped++;
            }
        }
        log.info("加载套餐模型关联: added={}, updated={}, skipped={}", added, updated, skipped);
    }

    // ===== upsert 方法 =====

    /**
     * 新增或更新供应商
     *
     * @param provider 待写入的供应商
     * @return "ADDED" | "UPDATED"
     */
    private String upsertProvider(Provider provider) {
        return providerGateway.findByCode(provider.getCode())
                .map(existing -> {
                    copyProviderFields(provider, existing);
                    providerGateway.save(existing);
                    return "UPDATED";
                })
                .orElseGet(() -> {
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
    private String upsertPlan(PlanCatalog catalog) {
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
    private String upsertPlanModel(PlanModelCatalog catalog) {
        return planModelCatalogGateway.findByPlanCodeAndModelName(
                        catalog.getPlanCode(), catalog.getModelName())
                .map(existing -> {
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
     * @param model 待写入的模型
     * @return "ADDED" | "UPDATED"
     */
    private String upsertModel(Model model) {
        return modelGateway.findByModelName(model.getModelName())
                .map(existing -> {
                    copyModelFields(model, existing);
                    modelGateway.save(existing);
                    return "UPDATED";
                })
                .orElseGet(() -> {
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

    // ===== 辅助方法 =====

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

    /**
     * 解析 capabilities JSON 对象为 Map
     */
    private Map<String, Boolean> parseCapabilities(Object capabilities) {
        if (capabilities == null) {
            return Map.of();
        }
        try {
            return catalogObjectMapper.convertValue(capabilities, new TypeReference<Map<String, Boolean>>() {});
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
            return catalogObjectMapper.convertValue(modalities, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            log.warn("解析 modalities 失败: {}", modalities, e);
            return List.of();
        }
    }

    // ===== JSON 数据 record（用于反序列化） =====

    record ProviderCatalogData(
        String providerCode, String providerName,
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