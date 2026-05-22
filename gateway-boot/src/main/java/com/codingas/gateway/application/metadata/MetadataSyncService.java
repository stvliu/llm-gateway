package com.codingas.gateway.application.metadata;

import com.codingas.gateway.application.metadata.dto.MetadataSyncResult;
import com.codingas.gateway.domain.metadata.entity.MetadataSource;
import com.codingas.gateway.domain.metadata.entity.ModelMetadata;
import com.codingas.gateway.domain.metadata.entity.ProductMetadata;
import com.codingas.gateway.domain.metadata.entity.ProviderMetadata;
import com.codingas.gateway.domain.metadata.enums.ProductType;
import com.codingas.gateway.domain.metadata.gateway.ModelMetadataGateway;
import com.codingas.gateway.domain.metadata.gateway.ModelsDevDataGateway;
import com.codingas.gateway.domain.metadata.gateway.ProductMetadataGateway;
import com.codingas.gateway.domain.metadata.gateway.ProviderMetadataGateway;
import com.codingas.gateway.domain.metadata.service.ModelMetadataDomainService;
import com.codingas.gateway.domain.metadata.service.ProductMetadataDomainService;
import com.codingas.gateway.infrastructure.metadata.config.MetadataSyncConfig;
import com.codingas.gateway.infrastructure.metadata.repository.BuiltinMetadataLoader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 元数据同步应用服务
 * <p>
 * 编排内置元数据同步和 Models.dev 同步用例。
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MetadataSyncService {

    private final BuiltinMetadataLoader builtinMetadataLoader;
    private final ModelsDevDataGateway modelsDevDataGateway;
    private final ProviderMetadataGateway providerMetadataGateway;
    private final ModelMetadataGateway modelMetadataGateway;
    private final ProductMetadataGateway productMetadataGateway;
    private final ModelMetadataDomainService modelMetadataDomainService;
    private final ProductMetadataDomainService productMetadataDomainService;
    private final MetadataSyncConfig config;

    /**
     * 同步内置元数据
     */
    @Transactional
    public MetadataSyncResult syncBuiltinMetadata() {
        log.info("Starting builtin metadata sync...");

        var providerResult = syncBuiltinProviders();
        var productResult = syncBuiltinProducts();
        var modelResult = syncBuiltinModels();

        log.info("Builtin metadata sync completed: {} providers ({} added, {} updated), {} products ({} added, {} updated), {} models ({} added, {} updated)",
            providerResult.total, providerResult.added, providerResult.updated,
            productResult.total, productResult.added, productResult.updated,
            modelResult.total, modelResult.added, modelResult.updated);

        return MetadataSyncResult.builder()
            .syncedCount(providerResult.total + productResult.total + modelResult.total)
            .addedCount(providerResult.added + productResult.added + modelResult.added)
            .updatedCount(providerResult.updated + productResult.updated + modelResult.updated)
            .syncedAt(Instant.now())
            .build();
    }

    /**
     * 同步内置供应商元数据
     */
    private SyncCounts syncBuiltinProviders() {
        List<Map<String, Object>> providers = builtinMetadataLoader.loadProviderMetadata();
        int added = 0, updated = 0;

        for (Map<String, Object> data : providers) {
            String providerId = (String) data.get("provider_id");
            try {
                ProviderMetadata existing = providerMetadataGateway.findByProviderId(providerId).orElse(null);

                if (existing == null) {
                    providerMetadataGateway.save(createProviderMetadata(data));
                    added++;
                } else {
                    updateProviderMetadata(existing, data);
                    providerMetadataGateway.save(existing);
                    updated++;
                }
            } catch (Exception e) {
                log.error("Failed to sync builtin provider metadata: {}", providerId, e);
            }
        }

        return new SyncCounts(providers.size(), added, updated);
    }

    /**
     * 同步内置模型元数据
     */
    private SyncCounts syncBuiltinModels() {
        List<Map<String, Object>> models = builtinMetadataLoader.loadModelMetadata();
        int added = 0, updated = 0;

        for (Map<String, Object> data : models) {
            String providerId = (String) data.get("provider_id");
            String modelId = (String) data.get("provider_model_id");
            try {
                ModelMetadata existing = modelMetadataGateway
                    .findByProviderIdAndModelId(providerId, modelId).orElse(null);

                if (existing == null) {
                    modelMetadataGateway.save(createModelMetadata(data));
                    added++;
                } else if (existing.getSource() == MetadataSource.BUILTIN) {
                    applyModelFields(existing, data);
                    existing.setUpdatedAt(Instant.now());
                    modelMetadataGateway.save(existing);
                    updated++;
                }
            } catch (Exception e) {
                log.error("Failed to sync builtin model metadata: {}/{}", providerId, modelId, e);
            }
        }

        return new SyncCounts(models.size(), added, updated);
    }

    /**
     * 同步内置产品元数据
     */
    private SyncCounts syncBuiltinProducts() {
        List<Map<String, Object>> products = builtinMetadataLoader.loadProductMetadata();
        int added = 0, updated = 0;

        for (Map<String, Object> data : products) {
            String providerId = (String) data.get("provider_id");
            String productName = (String) data.get("product_name");
            try {
                ProductMetadata existing = productMetadataGateway
                    .findByProviderIdAndProductName(providerId, productName).orElse(null);

                if (existing == null) {
                    productMetadataGateway.save(createProductMetadata(data));
                    added++;
                } else {
                    updateProductMetadata(existing, data);
                    productMetadataGateway.save(existing);
                    updated++;
                }
            } catch (Exception e) {
                log.error("Failed to sync builtin product metadata: {}/{}", providerId, productName, e);
            }
        }

        return new SyncCounts(products.size(), added, updated);
    }

    /**
     * 同步 Models.dev 数据
     */
    @Transactional
    public MetadataSyncResult syncModelsDev() {
        log.info("Starting Models.dev sync...");

        Map<String, List<ModelsDevDataGateway.ModelData>> externalData =
            modelsDevDataGateway.fetchAllSupportedModels();

        if (externalData.isEmpty()) {
            log.info("No data fetched from Models.dev");
            return buildEmptyResult();
        }

        // 收集待保存的实体
        List<ModelMetadata> toAdd = new ArrayList<>();
        List<ModelMetadata> toUpdate = new ArrayList<>();
        Set<String> activeKeys = new HashSet<>();
        int skippedCount = 0;

        for (Map.Entry<String, List<ModelsDevDataGateway.ModelData>> entry : externalData.entrySet()) {
            String providerId = entry.getKey();
            for (ModelsDevDataGateway.ModelData data : entry.getValue()) {
                String key = providerId + ":" + data.modelId();
                activeKeys.add(key);

                try {
                    ModelMetadata existing = modelMetadataGateway
                        .findByProviderIdAndModelId(providerId, data.modelId())
                        .orElse(null);

                    if (existing == null) {
                        toAdd.add(modelMetadataDomainService.createFromExternalData(providerId, data));
                    } else if (modelMetadataDomainService.canBeOverriddenBySync(existing)) {
                        modelMetadataDomainService.applyExternalData(existing, data);
                        toUpdate.add(existing);
                    } else {
                        skippedCount++;
                    }
                } catch (Exception e) {
                    log.error("Failed to sync model {}:{}", providerId, data.modelId(), e);
                }
            }
        }

        // 批量保存
        if (!toAdd.isEmpty()) {
            modelMetadataGateway.saveAll(toAdd);
        }
        if (!toUpdate.isEmpty()) {
            modelMetadataGateway.saveAll(toUpdate);
        }

        // 标记消失的模型
        int deprecatedCount = markDeprecatedModels(activeKeys);

        log.info("Models.dev sync completed: {} total, {} added, {} updated, {} skipped, {} deprecated",
            activeKeys.size(), toAdd.size(), toUpdate.size(), skippedCount, deprecatedCount);

        return MetadataSyncResult.builder()
            .syncedCount(activeKeys.size())
            .addedCount(toAdd.size())
            .updatedCount(toUpdate.size())
            .syncedAt(Instant.now())
            .build();
    }

    /**
     * 标记 Models.dev 中消失的模型为 DEPRECATED
     */
    private int markDeprecatedModels(Set<String> activeKeys) {
        List<ModelMetadata> toDeprecate = new ArrayList<>();
        List<ModelMetadata> existingModels = modelMetadataGateway.findBySource(MetadataSource.MODELS_DEV);

        for (ModelMetadata model : existingModels) {
            String key = model.getProviderId() + ":" + model.getProviderModelId();
            if (!activeKeys.contains(key) && model.getState() != null
                && !model.getState().name().equals("DEPRECATED")) {
                modelMetadataDomainService.markDeprecated(model);
                toDeprecate.add(model);
            }
        }

        if (!toDeprecate.isEmpty()) {
            modelMetadataGateway.saveAll(toDeprecate);
        }

        return toDeprecate.size();
    }

    /**
     * 手动触发全量同步
     */
    public MetadataSyncResult syncAll() {
        var builtinResult = syncBuiltinMetadata();
        MetadataSyncResult devResult = null;
        if (config.getModelsDev().isEnabled()) {
            devResult = syncModelsDev();
        }
        return MetadataSyncResult.builder()
            .syncedCount(builtinResult.getSyncedCount() + (devResult != null ? devResult.getSyncedCount() : 0))
            .addedCount(builtinResult.getAddedCount() + (devResult != null ? devResult.getAddedCount() : 0))
            .updatedCount(builtinResult.getUpdatedCount() + (devResult != null ? devResult.getUpdatedCount() : 0))
            .syncedAt(Instant.now())
            .build();
    }

    // ==================== 私有辅助方法 ====================

    private ProviderMetadata createProviderMetadata(Map<String, Object> data) {
        String providerId = (String) data.get("provider_id");
        ProviderMetadata metadata = new ProviderMetadata(
            providerId,
            (String) data.getOrDefault("provider_name", providerId),
            data.get("provider_config")
        );
        metadata.setDescription((String) data.get("description"));
        metadata.setIconUrl((String) data.get("icon_url"));
        metadata.setTags(data.get("tags"));
        metadata.setCreatedAt(Instant.now());
        metadata.setUpdatedAt(Instant.now());
        return metadata;
    }

    private void updateProviderMetadata(ProviderMetadata existing, Map<String, Object> data) {
        existing.setProviderName((String) data.getOrDefault("provider_name", existing.getProviderName()));
        existing.setProviderConfig(data.get("provider_config"));
        existing.setDescription((String) data.getOrDefault("description", existing.getDescription()));
        existing.setIconUrl((String) data.getOrDefault("icon_url", existing.getIconUrl()));
        existing.setTags(data.get("tags"));
        existing.setUpdatedAt(Instant.now());
    }

    @SuppressWarnings("unchecked")
    private ProductMetadata createProductMetadata(Map<String, Object> data) {
        String providerId = (String) data.get("provider_id");
        String productName = (String) data.get("product_name");
        String productTypeStr = (String) data.get("product_type");
        ProductType productType = productTypeStr != null ? ProductType.fromCode(productTypeStr) : ProductType.STANDARD;

        ProductMetadata metadata = new ProductMetadata(providerId, productName, productType);
        metadata.setEndpoints((Map<String, String>) data.get("endpoints"));
        metadata.setDescription((String) data.get("description"));
        metadata.setIsDefault((Boolean) data.getOrDefault("is_default", false));
        metadata.setCreatedAt(Instant.now());
        metadata.setUpdatedAt(Instant.now());
        return metadata;
    }

    @SuppressWarnings("unchecked")
    private void updateProductMetadata(ProductMetadata existing, Map<String, Object> data) {
        if (data.get("endpoints") != null) {
            existing.setEndpoints((Map<String, String>) data.get("endpoints"));
        }
        existing.setDescription((String) data.getOrDefault("description", existing.getDescription()));
        existing.setIsDefault((Boolean) data.getOrDefault("is_default", existing.getIsDefault()));
        existing.setUpdatedAt(Instant.now());
    }

    private ModelMetadata createModelMetadata(Map<String, Object> data) {
        String providerId = (String) data.get("provider_id");
        String modelId = (String) data.get("provider_model_id");
        ModelMetadata metadata = new ModelMetadata(
            providerId, modelId,
            (String) data.getOrDefault("display_name", modelId),
            MetadataSource.BUILTIN
        );
        applyModelFields(metadata, data);
        metadata.setCreatedAt(Instant.now());
        metadata.setUpdatedAt(Instant.now());
        return metadata;
    }

    @SuppressWarnings("unchecked")
    private void applyModelFields(ModelMetadata metadata, Map<String, Object> data) {
        if (data.get("display_name") != null) {
            metadata.setDisplayName((String) data.get("display_name"));
        }
        if (data.get("context_window") != null) {
            metadata.setContextWindow(((Number) data.get("context_window")).intValue());
        }
        if (data.get("input_price") != null) {
            metadata.setInputPrice(java.math.BigDecimal.valueOf(((Number) data.get("input_price")).doubleValue()));
        }
        if (data.get("output_price") != null) {
            metadata.setOutputPrice(java.math.BigDecimal.valueOf(((Number) data.get("output_price")).doubleValue()));
        }
        if (data.get("capabilities") != null) {
            metadata.setCapabilities((Map<String, Boolean>) data.get("capabilities"));
        }
    }

    private MetadataSyncResult buildEmptyResult() {
        return MetadataSyncResult.builder()
            .syncedCount(0)
            .addedCount(0)
            .updatedCount(0)
            .syncedAt(Instant.now())
            .build();
    }

    /**
     * 同步计数
     */
    private record SyncCounts(int total, int added, int updated) {}
}
