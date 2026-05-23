package com.codingas.gateway.infrastructure.metadata.gateway;

import com.codingas.gateway.domain.metadata.entity.ProviderMetadata;
import com.codingas.gateway.domain.metadata.enums.MetadataState;
import com.codingas.gateway.domain.metadata.gateway.ProviderMetadataGateway;
import com.codingas.gateway.infrastructure.metadata.database.ProviderMetadataDo;
import com.codingas.gateway.infrastructure.metadata.database.ProviderMetadataRepository;
import com.codingas.gateway.common.util.JsonUtils;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 供应商元数据网关实现
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProviderMetadataGatewayImpl implements ProviderMetadataGateway {

    private final ProviderMetadataRepository repository;

    @Override
    public ProviderMetadata save(ProviderMetadata metadata) {
        ProviderMetadataDo doEntity = toDo(metadata);
        ProviderMetadataDo saved = repository.save(doEntity);
        return toEntity(saved);
    }

    @Override
    public Optional<ProviderMetadata> findById(Long id) {
        return repository.findById(id).map(this::toEntity);
    }

    @Override
    public Optional<ProviderMetadata> findByProviderId(String providerId) {
        return repository.findByProviderId(providerId).map(this::toEntity);
    }

    @Override
    public Page<ProviderMetadata> findByConditions(
            String keyword, Pageable pageable) {
        Specification<ProviderMetadataDo> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            // 排除逻辑删除
            predicates.add(cb.isNull(root.get("deletedAt")));
            if (keyword != null && !keyword.isBlank()) {
                predicates.add(cb.or(
                    cb.like(root.get("providerName"), "%" + keyword + "%"),
                    cb.like(root.get("providerId"), "%" + keyword + "%")
                ));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return repository.findAll(spec, pageable).map(this::toEntity);
    }

    @Override
    public List<ProviderMetadata> findAllMetadata() {
        Specification<ProviderMetadataDo> spec = (root, query, cb) ->
            cb.isNull(root.get("deletedAt"));
        return repository.findAll(spec).stream().map(this::toEntity).toList();
    }

    @Override
    public boolean existsByProviderId(String providerId) {
        return repository.existsByProviderId(providerId);
    }

    @Override
    public void deleteById(Long id) {
        repository.deleteById(id);
    }

    // ==================== DO ↔ Entity 转换 ====================

    private ProviderMetadata toEntity(ProviderMetadataDo doEntity) {
        ProviderMetadata entity = new ProviderMetadata();
        entity.setId(doEntity.getId());
        entity.setProviderId(doEntity.getProviderId());
        entity.setProviderName(doEntity.getProviderName());
        entity.setProviderConfig(parseJson(doEntity.getProviderConfig()));
        entity.setIconUrl(doEntity.getIconUrl());
        entity.setDescription(doEntity.getDescription());
        entity.setTags(parseJson(doEntity.getTags()));
        entity.setState(doEntity.getState() != null ? MetadataState.valueOf(doEntity.getState()) : null);
        entity.setDeletedAt(doEntity.getDeletedAt());
        entity.setCreatedAt(doEntity.getCreatedAt());
        entity.setCreatedBy(doEntity.getCreatedBy());
        entity.setUpdatedAt(doEntity.getUpdatedAt());
        entity.setUpdatedBy(doEntity.getUpdatedBy());
        return entity;
    }

    private ProviderMetadataDo toDo(ProviderMetadata entity) {
        ProviderMetadataDo doEntity = new ProviderMetadataDo();
        doEntity.setId(entity.getId());
        doEntity.setProviderId(entity.getProviderId());
        doEntity.setProviderName(entity.getProviderName());
        doEntity.setProviderConfig(toJson(entity.getProviderConfig()));
        doEntity.setIconUrl(entity.getIconUrl());
        doEntity.setDescription(entity.getDescription());
        doEntity.setTags(toJson(entity.getTags()));
        doEntity.setState(entity.getState() != null ? entity.getState().name() : MetadataState.ACTIVE.name());
        doEntity.setDeletedAt(entity.getDeletedAt());
        doEntity.setCreatedAt(entity.getCreatedAt());
        doEntity.setCreatedBy(entity.getCreatedBy());
        doEntity.setUpdatedAt(entity.getUpdatedAt());
        doEntity.setUpdatedBy(entity.getUpdatedBy());
        return doEntity;
    }

    private Object parseJson(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return JsonUtils.readTree(json);
        } catch (Exception e) {
            log.warn("JSON 解析失败: {}", json, e);
            return null;
        }
    }

    private String toJson(Object obj) {
        if (obj == null) return null;
        return JsonUtils.toJson(obj);
    }
}
