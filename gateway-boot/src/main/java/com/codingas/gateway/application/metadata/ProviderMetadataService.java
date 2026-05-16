package com.codingas.gateway.application.metadata;

import com.codingas.gateway.application.metadata.dto.ApplyMetadataRequest;
import com.codingas.gateway.application.metadata.dto.ApplyMetadataResult;
import com.codingas.gateway.application.metadata.dto.MetadataCreateRequest;
import com.codingas.gateway.application.metadata.dto.MetadataUpdateRequest;
import com.codingas.gateway.application.metadata.dto.ProviderMetadataResponse;
import com.codingas.gateway.domain.metadata.entity.ProviderMetadata;
import com.codingas.gateway.domain.metadata.gateway.ModelMetadataGateway;
import com.codingas.gateway.domain.metadata.gateway.ProviderMetadataGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * 供应商元数据服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProviderMetadataService {

    private final ProviderMetadataGateway providerMetadataGateway;
    private final ModelMetadataGateway modelMetadataGateway;

    /**
     * 分页查询供应商元数据
     */
    public Page<ProviderMetadataResponse> listProviderMetadata(
            String providerType,
            String keyword, Pageable pageable) {
        Page<ProviderMetadata> page = providerMetadataGateway.findByConditions(
            providerType, keyword, pageable);
        return page.map(this::toResponse);
    }

    /**
     * 获取供应商元数据详情
     */
    public ProviderMetadataResponse getProviderMetadata(Long id) {
        ProviderMetadata metadata = providerMetadataGateway.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("供应商元数据不存在: id=" + id));
        return toResponse(metadata);
    }

    /**
     * 获取所有供应商元数据
     */
    public List<ProviderMetadataResponse> listAllMetadata() {
        return providerMetadataGateway.findAllMetadata().stream()
            .map(this::toResponse)
            .toList();
    }

    /**
     * 创建供应商元数据
     */
    @Transactional
    public ProviderMetadataResponse createMetadata(MetadataCreateRequest request) {
        if (providerMetadataGateway.existsByProviderId(request.getProviderId())) {
            throw new IllegalArgumentException("供应商标识已存在: " + request.getProviderId());
        }

        ProviderMetadata metadata = new ProviderMetadata(
            request.getProviderId(),
            request.getProviderName(),
            request.getProviderType(),
            request.getProviderConfig()
        );
        metadata.setDescription(request.getDescription());
        metadata.setIconUrl(request.getIconUrl());
        metadata.setTags(request.getTags());
        metadata.setCreatedAt(Instant.now());
        metadata.setUpdatedAt(Instant.now());

        ProviderMetadata saved = providerMetadataGateway.save(metadata);
        log.info("Created provider metadata: providerId={}", saved.getProviderId());
        return toResponse(saved);
    }

    /**
     * 更新供应商元数据
     */
    @Transactional
    public ProviderMetadataResponse updateMetadata(Long id, MetadataUpdateRequest request) {
        ProviderMetadata metadata = providerMetadataGateway.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("供应商元数据不存在: id=" + id));

        if (request.getProviderName() != null) metadata.setProviderName(request.getProviderName());
        if (request.getProviderConfig() != null) metadata.setProviderConfig(request.getProviderConfig());
        if (request.getDescription() != null) metadata.setDescription(request.getDescription());
        if (request.getIconUrl() != null) metadata.setIconUrl(request.getIconUrl());
        if (request.getTags() != null) metadata.setTags(request.getTags());
        metadata.setUpdatedAt(Instant.now());

        ProviderMetadata saved = providerMetadataGateway.save(metadata);
        log.info("Updated provider metadata: id={}, providerId={}", saved.getId(), saved.getProviderId());
        return toResponse(saved);
    }

    /**
     * 删除供应商元数据（逻辑删除）
     */
    @Transactional
    public void deleteMetadata(Long id) {
        providerMetadataGateway.deleteById(id);
        log.info("Deleted provider metadata: id={}", id);
    }

    /**
     * 应用元数据：创建供应商实例
     */
    @Transactional
    public ApplyMetadataResult applyMetadata(Long id, ApplyMetadataRequest request) {
        ProviderMetadata metadata = providerMetadataGateway.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("供应商元数据不存在: id=" + id));

        // 获取关联的模型元数据
        var models = modelMetadataGateway.findByProviderId(metadata.getProviderId());

        // TODO: 调用 Provider 创建服务，创建 Provider + Model + ApiKey
        // 当前仅返回元数据信息，实际创建逻辑在 Provider 领域完成
        log.info("Applied provider metadata: providerId={}, models={}", metadata.getProviderId(), models.size());

        return ApplyMetadataResult.builder()
            .providerId(metadata.getId())
            .providerName(metadata.getProviderName())
            .modelIds(models.stream().map(m -> m.getId()).toList())
            .modelNames(models.stream().map(m -> m.getDisplayName()).toList())
            .createdAt(Instant.now())
            .build();
    }

    private ProviderMetadataResponse toResponse(ProviderMetadata metadata) {
        int modelCount = modelMetadataGateway.findByProviderId(metadata.getProviderId()).size();
        return ProviderMetadataResponse.builder()
            .id(metadata.getId())
            .providerId(metadata.getProviderId())
            .providerName(metadata.getProviderName())
            .providerType(metadata.getProviderType())
            .providerConfig(metadata.getProviderConfig())
            .iconUrl(metadata.getIconUrl())
            .description(metadata.getDescription())
            .tags(metadata.getTags())
            .state(metadata.getState() != null ? metadata.getState().name() : null)
            .createdAt(metadata.getCreatedAt())
            .updatedAt(metadata.getUpdatedAt())
            .modelCount(modelCount)
            .build();
    }
}
