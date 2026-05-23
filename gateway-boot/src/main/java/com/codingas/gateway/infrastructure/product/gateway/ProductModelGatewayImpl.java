package com.codingas.gateway.infrastructure.product.gateway;

import com.codingas.gateway.domain.product.entity.ProductModel;
import com.codingas.gateway.domain.product.gateway.ProductModelGateway;
import com.codingas.gateway.infrastructure.product.gateway.database.dataobject.ProductModelDo;
import com.codingas.gateway.infrastructure.product.gateway.database.repository.ProductModelRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * 产品-模型关联网关实现
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProductModelGatewayImpl implements ProductModelGateway {

    private final ProductModelRepository repository;

    @Override
    public ProductModel save(ProductModel association) {
        ProductModelDo doEntity = toDo(association);
        ProductModelDo saved = repository.save(doEntity);
        return toEntity(saved);
    }

    @Override
    public Optional<ProductModel> findById(Long id) {
        return repository.findById(id).map(this::toEntity);
    }

    @Override
    public List<ProductModel> findByProductId(Long productId) {
        return repository.findByProductId(productId).stream().map(this::toEntity).toList();
    }

    @Override
    public List<ProductModel> findByModelId(Long modelId) {
        return repository.findByModelId(modelId).stream().map(this::toEntity).toList();
    }

    @Override
    public Optional<ProductModel> findByProductIdAndModelId(Long productId, Long modelId) {
        return repository.findByProductIdAndModelId(productId, modelId).map(this::toEntity);
    }

    @Override
    public List<ProductModel> saveAll(List<ProductModel> associations) {
        List<ProductModelDo> doList = associations.stream().map(this::toDo).toList();
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

    private ProductModel toEntity(ProductModelDo doEntity) {
        ProductModel entity = new ProductModel();
        entity.setId(doEntity.getId());
        entity.setProductId(doEntity.getProductId());
        entity.setModelId(doEntity.getModelId());
        entity.setCreatedBy(doEntity.getCreatedBy());
        entity.setCreatedAt(doEntity.getCreatedAt());
        entity.setUpdatedBy(doEntity.getUpdatedBy());
        entity.setUpdatedAt(doEntity.getUpdatedAt());
        return entity;
    }

    private ProductModelDo toDo(ProductModel entity) {
        ProductModelDo doEntity = new ProductModelDo();
        doEntity.setId(entity.getId());
        doEntity.setProductId(entity.getProductId());
        doEntity.setModelId(entity.getModelId());
        return doEntity;
    }
}