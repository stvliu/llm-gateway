package com.codingas.gateway.domain.metadata.service;

import com.codingas.gateway.domain.metadata.entity.MetadataSource;
import com.codingas.gateway.domain.metadata.entity.ModelMetadata;
import com.codingas.gateway.domain.metadata.enums.MetadataState;
import com.codingas.gateway.domain.metadata.gateway.ModelsDevDataGateway;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 模型元数据领域服务
 * <p>
 * 封装模型元数据的业务规则，保持实体仅含 Getter/Setter。
 * </p>
 */
@Service
public class ModelMetadataDomainService {

    /**
     * 判断是否可被同步覆盖
     * <p>
     * 仅 BUILTIN 或 MODELS_DEV 来源的记录可被外部同步更新，
     * MANUAL 和 OVERRIDE 的记录不会被覆盖。
     * </p>
     */
    public boolean canBeOverriddenBySync(ModelMetadata metadata) {
        return metadata.getSource() == MetadataSource.BUILTIN
            || metadata.getSource() == MetadataSource.MODELS_DEV;
    }

    /**
     * 标记为已同步
     */
    public void markSynced(ModelMetadata metadata) {
        metadata.setSource(MetadataSource.MODELS_DEV);
        metadata.setSourceSyncedAt(Instant.now());
        // updatedAt 由 JPA 审计自动设置
    }

    /**
     * 标记为已废弃（上游数据源中消失）
     */
    public void markDeprecated(ModelMetadata metadata) {
        metadata.setState(MetadataState.DEPRECATED);
        // updatedAt 由 JPA 审计自动设置
    }

    /**
     * 从外部数据创建新模型元数据
     */
    public ModelMetadata createFromExternalData(String providerId, ModelsDevDataGateway.ModelData data) {
        ModelMetadata metadata = new ModelMetadata(
            providerId,
            data.modelId(),
            data.displayName(),
            MetadataSource.MODELS_DEV
        );

        applyPricingFields(metadata, data);
        applyContextFields(metadata, data);
        applyMetaFields(metadata, data);

        metadata.setSourceSyncedAt(Instant.now());
        // createdAt 由 JPA 审计自动设置

        // 能力
        metadata.setCapabilities(buildCapabilities(data));

        // 模态
        metadata.setModalities(buildModalities(data));

        return metadata;
    }

    /**
     * 应用外部数据到已有模型元数据
     */
    public void applyExternalData(ModelMetadata metadata, ModelsDevDataGateway.ModelData data) {
        applyIfNotNull(data.displayName(), metadata::setDisplayName);
        applyPricingFields(metadata, data);
        applyContextFields(metadata, data);
        applyMetaFields(metadata, data);

        metadata.setSource(MetadataSource.MODELS_DEV);
        metadata.setSourceSyncedAt(Instant.now());
        // updatedAt 由 JPA 审计自动设置
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 应用定价字段
     */
    private void applyPricingFields(ModelMetadata metadata, ModelsDevDataGateway.ModelData data) {
        applyIfNotNull(data.inputPrice(), metadata::setInputPrice);
        applyIfNotNull(data.outputPrice(), metadata::setOutputPrice);
        applyIfNotNull(data.reasoningPrice(), metadata::setReasoningPrice);
        applyIfNotNull(data.cacheReadPrice(), metadata::setCacheReadPrice);
        applyIfNotNull(data.cacheWritePrice(), metadata::setCacheWritePrice);
        applyIfNotNull(data.inputAudioPrice(), metadata::setInputAudioPrice);
        applyIfNotNull(data.outputAudioPrice(), metadata::setOutputAudioPrice);
    }

    /**
     * 应用上下文字段
     */
    private void applyContextFields(ModelMetadata metadata, ModelsDevDataGateway.ModelData data) {
        applyIfNotNull(data.contextWindow(), metadata::setContextWindow);
        applyIfNotNull(data.maxInputTokens(), metadata::setMaxInputTokens);
        applyIfNotNull(data.maxOutputTokens(), metadata::setMaxOutputTokens);
    }

    /**
     * 应用元数据字段
     */
    private void applyMetaFields(ModelMetadata metadata, ModelsDevDataGateway.ModelData data) {
        applyIfNotNull(data.knowledgeCutoff(), metadata::setKnowledgeCutoff);
        applyIfNotNull(data.openWeights(), metadata::setOpenWeights);
        applyIfNotNull(data.family(), metadata::setModelFamily);
    }

    /**
     * 构建能力映射
     */
    private Map<String, Boolean> buildCapabilities(ModelsDevDataGateway.ModelData data) {
        Map<String, Boolean> capabilities = new HashMap<>();
        capabilities.put("vision", Boolean.TRUE.equals(data.vision()));
        capabilities.put("function_calling", Boolean.TRUE.equals(data.functionCalling()));
        capabilities.put("streaming", true);
        // 根据音频定价推断音频能力
        capabilities.put("audio", data.inputAudioPrice() != null || data.outputAudioPrice() != null);
        return capabilities;
    }

    /**
     * 构建模态列表
     */
    private List<String> buildModalities(ModelsDevDataGateway.ModelData data) {
        List<String> modalities = new ArrayList<>();
        modalities.add("text");

        if (Boolean.TRUE.equals(data.vision())) {
            modalities.add("image");
        }

        // 根据音频定价推断音频模态
        if (data.inputAudioPrice() != null || data.outputAudioPrice() != null) {
            modalities.add("audio");
        }

        return modalities;
    }

    /**
     * 非空则应用
     */
    private <T> void applyIfNotNull(T value, Consumer<T> setter) {
        if (value != null) {
            setter.accept(value);
        }
    }
}
