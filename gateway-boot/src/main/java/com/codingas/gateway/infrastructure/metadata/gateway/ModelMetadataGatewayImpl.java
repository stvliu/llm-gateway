package com.codingas.gateway.infrastructure.metadata.gateway;

import com.codingas.gateway.domain.metadata.entity.MetadataSource;
import com.codingas.gateway.domain.metadata.entity.ModelMetadata;
import com.codingas.gateway.domain.metadata.enums.MetadataState;
import com.codingas.gateway.domain.metadata.gateway.ModelMetadataGateway;
import com.codingas.gateway.infrastructure.metadata.database.ModelMetadataDo;
import com.codingas.gateway.infrastructure.metadata.database.ModelMetadataRepository;
import com.codingas.gateway.common.util.JsonUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 模型元数据网关实现
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ModelMetadataGatewayImpl implements ModelMetadataGateway {

    private final ModelMetadataRepository repository;

    @Override
    public ModelMetadata save(ModelMetadata metadata) {
        ModelMetadataDo doEntity = toDo(metadata);
        ModelMetadataDo saved = repository.save(doEntity);
        return toEntity(saved);
    }

    @Override
    public Optional<ModelMetadata> findById(Long id) {
        return repository.findById(id).map(this::toEntity);
    }

    @Override
    public List<ModelMetadata> findByProviderId(String providerId) {
        return repository.findByProviderId(providerId).stream().map(this::toEntity).toList();
    }

    @Override
    public List<ModelMetadata> findByProductId(Long productId) {
        return repository.findByProductId(productId).stream().map(this::toEntity).toList();
    }

    @Override
    public Optional<ModelMetadata> findByProviderIdAndModelId(String providerId, String providerModelId) {
        return repository.findByProviderIdAndProviderModelId(providerId, providerModelId).map(this::toEntity);
    }

    @Override
    public List<ModelMetadata> findBySource(MetadataSource source) {
        return repository.findBySource(source.name()).stream().map(this::toEntity).toList();
    }

    @Override
    public Page<ModelMetadata> findByConditions(
            String providerId, String keyword,
            MetadataSource source, Pageable pageable) {
        Specification<ModelMetadataDo> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (providerId != null && !providerId.isBlank()) {
                predicates.add(cb.equal(root.get("providerId"), providerId));
            }
            if (keyword != null && !keyword.isBlank()) {
                predicates.add(cb.or(
                    cb.like(root.get("displayName"), "%" + keyword + "%"),
                    cb.like(root.get("providerModelId"), "%" + keyword + "%")
                ));
            }
            if (source != null) {
                predicates.add(cb.equal(root.get("source"), source.name()));
            }
            predicates.add(cb.equal(root.get("state"), MetadataState.ACTIVE.name()));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return repository.findAll(spec, pageable).map(this::toEntity);
    }

    @Override
    public List<ModelMetadata> saveAll(List<ModelMetadata> metadataList) {
        List<ModelMetadataDo> doList = metadataList.stream().map(this::toDo).toList();
        List<ModelMetadataDo> saved = repository.saveAll(doList);
        return saved.stream().map(this::toEntity).toList();
    }

    @Override
    public void deleteById(Long id) {
        repository.deleteById(id);
    }

    @Override
    public boolean existsByProviderIdAndModelId(String providerId, String providerModelId) {
        return repository.existsByProviderIdAndProviderModelId(providerId, providerModelId);
    }

    // ==================== DO ↔ Entity 转换 ====================

    private ModelMetadata toEntity(ModelMetadataDo doEntity) {
        ModelMetadata entity = new ModelMetadata();
        entity.setId(doEntity.getId());
        entity.setProductId(doEntity.getProductId());
        entity.setProviderId(doEntity.getProviderId());
        entity.setProviderModelId(doEntity.getProviderModelId());
        entity.setDisplayName(doEntity.getDisplayName());
        entity.setModelFamily(doEntity.getModelFamily());
        entity.setContextWindow(doEntity.getContextWindow());
        entity.setMaxInputTokens(doEntity.getMaxInputTokens());
        entity.setMaxOutputTokens(doEntity.getMaxOutputTokens());
        entity.setInputPrice(doEntity.getInputPrice());
        entity.setOutputPrice(doEntity.getOutputPrice());
        entity.setReasoningPrice(doEntity.getReasoningPrice());
        entity.setCacheReadPrice(doEntity.getCacheReadPrice());
        entity.setCacheWritePrice(doEntity.getCacheWritePrice());
        entity.setInputAudioPrice(doEntity.getInputAudioPrice());
        entity.setOutputAudioPrice(doEntity.getOutputAudioPrice());
        entity.setKnowledgeCutoff(doEntity.getKnowledgeCutoff());
        entity.setReleaseDate(doEntity.getReleaseDate());
        entity.setOpenWeights(doEntity.getOpenWeights());
        entity.setModalities(parseList(doEntity.getModalities()));
        entity.setCapabilities(parseMap(doEntity.getCapabilities()));
        entity.setSource(doEntity.getSource() != null ? MetadataSource.valueOf(doEntity.getSource()) : MetadataSource.BUILTIN);
        entity.setSourceSyncedAt(doEntity.getSourceSyncedAt());
        entity.setState(doEntity.getState() != null ? MetadataState.valueOf(doEntity.getState()) : MetadataState.ACTIVE);
        entity.setCreatedAt(doEntity.getCreatedAt());
        entity.setCreatedBy(doEntity.getCreatedBy());
        entity.setUpdatedAt(doEntity.getUpdatedAt());
        entity.setUpdatedBy(doEntity.getUpdatedBy());
        return entity;
    }

    private ModelMetadataDo toDo(ModelMetadata entity) {
        ModelMetadataDo doEntity = new ModelMetadataDo();
        doEntity.setId(entity.getId());
        doEntity.setProductId(entity.getProductId());
        doEntity.setProviderId(entity.getProviderId());
        doEntity.setProviderModelId(entity.getProviderModelId());
        doEntity.setDisplayName(entity.getDisplayName());
        doEntity.setModelFamily(entity.getModelFamily());
        doEntity.setContextWindow(entity.getContextWindow());
        doEntity.setMaxInputTokens(entity.getMaxInputTokens());
        doEntity.setMaxOutputTokens(entity.getMaxOutputTokens());
        doEntity.setInputPrice(entity.getInputPrice());
        doEntity.setOutputPrice(entity.getOutputPrice());
        doEntity.setReasoningPrice(entity.getReasoningPrice());
        doEntity.setCacheReadPrice(entity.getCacheReadPrice());
        doEntity.setCacheWritePrice(entity.getCacheWritePrice());
        doEntity.setInputAudioPrice(entity.getInputAudioPrice());
        doEntity.setOutputAudioPrice(entity.getOutputAudioPrice());
        doEntity.setKnowledgeCutoff(entity.getKnowledgeCutoff());
        doEntity.setReleaseDate(entity.getReleaseDate());
        doEntity.setOpenWeights(entity.getOpenWeights());
        doEntity.setModalities(toJson(entity.getModalities()));
        doEntity.setCapabilities(toJson(entity.getCapabilities()));
        doEntity.setSource(entity.getSource() != null ? entity.getSource().name() : MetadataSource.BUILTIN.name());
        doEntity.setSourceSyncedAt(entity.getSourceSyncedAt());
        doEntity.setState(entity.getState() != null ? entity.getState().name() : MetadataState.ACTIVE.name());
        doEntity.setCreatedAt(entity.getCreatedAt());
        doEntity.setCreatedBy(entity.getCreatedBy());
        doEntity.setUpdatedAt(entity.getUpdatedAt());
        doEntity.setUpdatedBy(entity.getUpdatedBy());
        return doEntity;
    }

    private Map<String, Boolean> parseMap(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            // 处理 H2 可能对 JSON 列双重序列化导致的引号包裹
            String normalized = json.trim();
            if (normalized.startsWith("\"") && normalized.endsWith("\"")) {
                normalized = JsonUtils.fromJson(normalized, String.class);
            }
            return JsonUtils.fromJson(normalized, new TypeReference<Map<String, Boolean>>() {});
        } catch (Exception e) {
            log.warn("JSON Map 解析失败: {}", json, e);
            return null;
        }
    }

    private List<String> parseList(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            // 处理 H2 可能对 JSON 列双重序列化导致的引号包裹
            String normalized = json.trim();
            if (normalized.startsWith("\"") && normalized.endsWith("\"")) {
                normalized = JsonUtils.fromJson(normalized, String.class);
            }
            return JsonUtils.fromJson(normalized, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            log.warn("JSON List 解析失败: {}", json, e);
            return null;
        }
    }

    private String toJson(Object obj) {
        if (obj == null) return null;
        String result = JsonUtils.toJson(obj);
        return result;
    }
}