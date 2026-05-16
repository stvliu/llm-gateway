package com.codingas.gateway.application.metadata;

import com.codingas.gateway.application.metadata.dto.ModelMetadataResponse;
import com.codingas.gateway.domain.metadata.entity.MetadataSource;
import com.codingas.gateway.domain.metadata.entity.ModelMetadata;
import com.codingas.gateway.domain.metadata.gateway.ModelMetadataGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * 模型元数据服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ModelMetadataService {

    private final ModelMetadataGateway modelMetadataGateway;

    /**
     * 分页查询模型元数据
     */
    public Page<ModelMetadataResponse> listModelMetadata(
            String providerId, String keyword,
            MetadataSource source, Pageable pageable) {
        Page<ModelMetadata> page = modelMetadataGateway.findByConditions(
            providerId, keyword, source, pageable);
        return page.map(this::toResponse);
    }

    /**
     * 获取模型元数据详情
     */
    public ModelMetadataResponse getModelMetadata(Long id) {
        ModelMetadata metadata = modelMetadataGateway.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("模型元数据不存在: id=" + id));
        return toResponse(metadata);
    }

    /**
     * 查询某供应商的所有模型
     */
    public List<ModelMetadataResponse> listByProviderId(String providerId) {
        return modelMetadataGateway.findByProviderId(providerId).stream()
            .map(this::toResponse)
            .toList();
    }

    /**
     * 创建模型元数据
     */
    @Transactional
    public ModelMetadataResponse createModelMetadata(ModelMetadata metadata) {
        if (modelMetadataGateway.existsByProviderIdAndModelId(
                metadata.getProviderId(), metadata.getProviderModelId())) {
            throw new IllegalArgumentException(
                "模型元数据已存在: " + metadata.getProviderId() + "/" + metadata.getProviderModelId());
        }
        ModelMetadata saved = modelMetadataGateway.save(metadata);
        log.info("Created model metadata: {}/{}", saved.getProviderId(), saved.getProviderModelId());
        return toResponse(saved);
    }

    /**
     * 更新模型元数据
     */
    @Transactional
    public ModelMetadataResponse updateModelMetadata(Long id, ModelMetadata update) {
        ModelMetadata existing = modelMetadataGateway.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("模型元数据不存在: id=" + id));

        // 仅更新非空字段
        if (update.getDisplayName() != null) existing.setDisplayName(update.getDisplayName());
        if (update.getContextWindow() != null) existing.setContextWindow(update.getContextWindow());
        if (update.getInputPrice() != null) existing.setInputPrice(update.getInputPrice());
        if (update.getOutputPrice() != null) existing.setOutputPrice(update.getOutputPrice());
        if (update.getCapabilities() != null) existing.setCapabilities(update.getCapabilities());
        if (update.getModalities() != null) existing.setModalities(update.getModalities());

        ModelMetadata saved = modelMetadataGateway.save(existing);
        log.info("Updated model metadata: {}/{}", saved.getProviderId(), saved.getProviderModelId());
        return toResponse(saved);
    }

    /**
     * 删除模型元数据
     */
    @Transactional
    public void deleteModelMetadata(Long id) {
        modelMetadataGateway.deleteById(id);
        log.info("Deleted model metadata: id={}", id);
    }

    private ModelMetadataResponse toResponse(ModelMetadata metadata) {
        return ModelMetadataResponse.builder()
            .id(metadata.getId())
            .providerId(metadata.getProviderId())
            .providerModelId(metadata.getProviderModelId())
            .displayName(metadata.getDisplayName())
            .modelFamily(metadata.getModelFamily())
            .contextWindow(metadata.getContextWindow())
            .maxInputTokens(metadata.getMaxInputTokens())
            .maxOutputTokens(metadata.getMaxOutputTokens())
            .inputPrice(metadata.getInputPrice())
            .outputPrice(metadata.getOutputPrice())
            .reasoningPrice(metadata.getReasoningPrice())
            .cacheReadPrice(metadata.getCacheReadPrice())
            .cacheWritePrice(metadata.getCacheWritePrice())
            .inputAudioPrice(metadata.getInputAudioPrice())
            .outputAudioPrice(metadata.getOutputAudioPrice())
            .knowledgeCutoff(metadata.getKnowledgeCutoff())
            .releaseDate(metadata.getReleaseDate())
            .openWeights(metadata.getOpenWeights())
            .modalities(metadata.getModalities())
            .capabilities(metadata.getCapabilities())
            .source(metadata.getSource() != null ? metadata.getSource().name() : null)
            .sourceSyncedAt(metadata.getSourceSyncedAt())
            .state(metadata.getState() != null ? metadata.getState().name() : null)
            .createdAt(metadata.getCreatedAt())
            .updatedAt(metadata.getUpdatedAt())
            .build();
    }
}