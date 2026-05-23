package com.codingas.gateway.infrastructure.metadata.gateway;

import com.codingas.gateway.domain.metadata.entity.MetadataSource;
import com.codingas.gateway.domain.metadata.entity.ProductModelMetadata;
import com.codingas.gateway.domain.metadata.enums.MetadataState;
import com.codingas.gateway.domain.metadata.gateway.ProductModelMetadataGateway;
import com.codingas.gateway.infrastructure.metadata.database.ProductModelMetadataDo;
import com.codingas.gateway.infrastructure.metadata.database.ProductModelMetadataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * 产品-模型元数据关联网关实现
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProductModelMetadataGatewayImpl implements ProductModelMetadataGateway {

    private final ProductModelMetadataRepository repository;

    @Override
    public ProductModelMetadata save(ProductModelMetadata association) {
        ProductModelMetadataDo doEntity = toDo(association);
        ProductModelMetadataDo saved = repository.save(doEntity);
        return toEntity(saved);
    }

    @Override
    public Optional<ProductModelMetadata> findById(Long id) {
        return repository.findById(id).map(this::toEntity);
    }

    @Override
    public List<ProductModelMetadata> findByProductId(Long productId) {
        return repository.findByProductId(productId).stream().map(this::toEntity).toList();
    }

    @Override
    public List<ProductModelMetadata> findByModelId(Long modelId) {
        return repository.findByModelId(modelId).stream().map(this::toEntity).toList();
    }

    @Override
    public Optional<ProductModelMetadata> findByProductIdAndModelId(Long productId, Long modelId) {
        return repository.findByProductIdAndModelId(productId, modelId).map(this::toEntity);
    }

    @Override
    public List<ProductModelMetadata> saveAll(List<ProductModelMetadata> associations) {
        List<ProductModelMetadataDo> doList = associations.stream().map(this::toDo).toList();
        return repository.saveAll(doList).stream().map(this::toEntity).toList();
    }

    @Override
    public void deleteById(Long id) {
        repository.deleteById(id);
    }

    @Override
    public void deleteByProductId(Long productId) {
        repository.deleteByProductId(productId);
    }

    @Override
    public void deleteByModelId(Long modelId) {
        repository.deleteByModelId(modelId);
    }

    @Override
    public boolean existsByProductIdAndModelId(Long productId, Long modelId) {
        return repository.existsByProductIdAndModelId(productId, modelId);
    }

    // ==================== DO ↔ Entity 转换 ====================

    private ProductModelMetadata toEntity(ProductModelMetadataDo doEntity) {
        ProductModelMetadata entity = new ProductModelMetadata();
        entity.setId(doEntity.getId());
        entity.setProductId(doEntity.getProductId());
        entity.setModelId(doEntity.getModelId());
        entity.setSource(parseMetadataSource(doEntity.getSource()));
        entity.setSourceSyncedAt(doEntity.getSourceSyncedAt());
        entity.setState(parseMetadataState(doEntity.getState()));
        entity.setCreatedAt(doEntity.getCreatedAt());
        entity.setCreatedBy(doEntity.getCreatedBy());
        entity.setUpdatedAt(doEntity.getUpdatedAt());
        entity.setUpdatedBy(doEntity.getUpdatedBy());
        return entity;
    }

    private ProductModelMetadataDo toDo(ProductModelMetadata entity) {
        ProductModelMetadataDo doEntity = new ProductModelMetadataDo();
        doEntity.setId(entity.getId());
        doEntity.setProductId(entity.getProductId());
        doEntity.setModelId(entity.getModelId());
        doEntity.setSource(entity.getSource() != null ? entity.getSource().name() : MetadataSource.BUILTIN.name());
        doEntity.setSourceSyncedAt(entity.getSourceSyncedAt());
        doEntity.setState(entity.getState() != null ? entity.getState().name() : MetadataState.ACTIVE.name());
        doEntity.setCreatedAt(entity.getCreatedAt());
        doEntity.setCreatedBy(entity.getCreatedBy());
        doEntity.setUpdatedAt(entity.getUpdatedAt());
        doEntity.setUpdatedBy(entity.getUpdatedBy());
        return doEntity;
    }

    private MetadataState parseMetadataState(String value) {
        if (value == null || value.isBlank()) return MetadataState.ACTIVE;
        try {
            return MetadataState.valueOf(value);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid MetadataState value: {}, defaulting to ACTIVE", value);
            return MetadataState.ACTIVE;
        }
    }

    private MetadataSource parseMetadataSource(String value) {
        if (value == null || value.isBlank()) return MetadataSource.BUILTIN;
        try {
            return MetadataSource.valueOf(value);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid MetadataSource value: {}, defaulting to BUILTIN", value);
            return MetadataSource.BUILTIN;
        }
    }
}
