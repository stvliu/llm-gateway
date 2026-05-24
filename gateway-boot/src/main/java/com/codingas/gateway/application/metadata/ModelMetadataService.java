package com.codingas.gateway.application.metadata;

import com.codingas.gateway.application.metadata.dto.ModelMetadataCreateRequest;
import com.codingas.gateway.application.metadata.dto.ModelMetadataResponse;
import com.codingas.gateway.application.metadata.dto.ModelMetadataUpdateRequest;
import com.codingas.gateway.domain.metadata.entity.MetadataSource;
import com.codingas.gateway.domain.metadata.entity.ModelMetadata;
import com.codingas.gateway.domain.metadata.gateway.ModelMetadataGateway;
import com.codingas.gateway.domain.supply.entity.ModelSpec;
import com.codingas.gateway.domain.supply.enums.ModelSpecState;
import com.codingas.gateway.domain.supply.gateway.ModelSpecGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * 模型元数据服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ModelMetadataService {

    private final ModelMetadataGateway modelMetadataGateway;
    private final ModelSpecGateway modelSpecGateway;

    /**
     * 分页查询模型元数据
     */
    public List<ModelMetadataResponse> listModelMetadata(String keyword, String providerId) {
        List<ModelMetadata> metadatas;
        if (providerId != null && !providerId.isBlank()) {
            metadatas = modelMetadataGateway.findByProviderId(providerId);
        } else {
            // 无过滤条件时使用 findByConditions 传入 null 参数
            metadatas = modelMetadataGateway.findByConditions(null, null, null, org.springframework.data.domain.Pageable.unpaged()).getContent();
        }
        return metadatas.stream().map(this::toResponse).toList();
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
     * 根据供应商标识查询模型元数据
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
    public ModelMetadataResponse createModelMetadata(ModelMetadataCreateRequest request) {
        ModelMetadata metadata = new ModelMetadata(
            request.providerId(),
            request.providerModelId(),
            request.displayName(),
            MetadataSource.MANUAL
        );
        metadata.setContextWindow(request.contextWindow());
        metadata.setCapabilities(request.capabilities());
        metadata.setCreatedAt(Instant.now());
        metadata.setUpdatedAt(Instant.now());

        ModelMetadata saved = modelMetadataGateway.save(metadata);
        log.info("Created model metadata: providerId={}, modelId={}", saved.getProviderId(), saved.getProviderModelId());
        return toResponse(saved);
    }

    /**
     * 更新模型元数据
     */
    @Transactional
    public ModelMetadataResponse updateModelMetadata(Long id, ModelMetadataUpdateRequest request) {
        ModelMetadata metadata = modelMetadataGateway.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("模型元数据不存在: id=" + id));

        if (request.displayName() != null) metadata.setDisplayName(request.displayName());
        if (request.contextWindow() != null) metadata.setContextWindow(request.contextWindow());
        if (request.capabilities() != null) metadata.setCapabilities(request.capabilities());
        metadata.setUpdatedAt(Instant.now());

        ModelMetadata saved = modelMetadataGateway.save(metadata);
        log.info("Updated model metadata: id={}", saved.getId());
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

    /**
     * 应用模型元数据：创建 ModelSpec
     */
    @Transactional
    public ModelSpec applyMetadata(Long id) {
        ModelMetadata metadata = modelMetadataGateway.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("模型元数据不存在: id=" + id));

        ModelSpec modelSpec = new ModelSpec();
        modelSpec.setProviderModelId(metadata.getProviderModelId());
        modelSpec.setDisplayName(metadata.getDisplayName());
        modelSpec.setContextWindow(metadata.getContextWindow());
        modelSpec.setCapabilities(metadata.getCapabilities());
        modelSpec.setState(ModelSpecState.ACTIVE);

        ModelSpec saved = modelSpecGateway.save(modelSpec);
        log.info("Applied model metadata: modelSpecId={}, providerModelId={}", saved.getId(), saved.getProviderModelId());
        return saved;
    }

    private ModelMetadataResponse toResponse(ModelMetadata metadata) {
        return ModelMetadataResponse.builder()
            .id(metadata.getId())
            .providerId(metadata.getProviderId())
            .providerModelId(metadata.getProviderModelId())
            .displayName(metadata.getDisplayName())
            .contextWindow(metadata.getContextWindow())
            .capabilities(metadata.getCapabilities())
            .createdAt(metadata.getCreatedAt())
            .updatedAt(metadata.getUpdatedAt())
            .build();
    }
}