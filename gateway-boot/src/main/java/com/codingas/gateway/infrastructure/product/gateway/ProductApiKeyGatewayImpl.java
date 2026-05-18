package com.codingas.gateway.infrastructure.product.gateway;

import com.codingas.gateway.domain.product.entity.ProductApiKey;
import com.codingas.gateway.domain.product.enums.ProductApiKeyState;
import com.codingas.gateway.domain.product.gateway.ProductApiKeyGateway;
import com.codingas.gateway.infrastructure.product.gateway.database.dataobject.ProductApiKeyDo;
import com.codingas.gateway.infrastructure.product.gateway.database.repository.ProductApiKeyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

/**
 * 产品 API Key Gateway 实现
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ProductApiKeyGatewayImpl implements ProductApiKeyGateway {

    private final ProductApiKeyRepository productApiKeyRepository;

    @Override
    public ProductApiKey save(ProductApiKey apiKey) {
        ProductApiKeyDo dataObject = toDataObject(apiKey);
        if (apiKey.getId() == null) {
            dataObject.setCreatedAt(LocalDateTime.now());
        }
        dataObject.setUpdatedAt(LocalDateTime.now());
        ProductApiKeyDo saved = productApiKeyRepository.save(dataObject);
        return toEntity(saved);
    }

    @Override
    public Optional<ProductApiKey> findById(Long id) {
        return productApiKeyRepository.findById(id).map(this::toEntity);
    }

    @Override
    public List<ProductApiKey> findActiveByProductId(Long productId) {
        return productApiKeyRepository.findActiveByProductId(productId).stream()
            .map(this::toEntity)
            .toList();
    }

    @Override
    public Optional<ProductApiKey> findDefaultByProductId(Long productId) {
        return productApiKeyRepository.findDefaultByProductId(productId)
            .map(this::toEntity);
    }

    @Override
    public void updateLastUsedAt(Long id) {
        productApiKeyRepository.updateLastUsedAt(id, LocalDateTime.now());
    }

    @Override
    public void deleteById(Long id) {
        productApiKeyRepository.deleteById(id);
    }

    @Override
    public long countActiveByProductId(Long productId) {
        return productApiKeyRepository.countByProductIdAndState(productId, "active");
    }

    private ProductApiKey toEntity(ProductApiKeyDo dataObject) {
        ProductApiKey entity = new ProductApiKey();
        entity.setId(dataObject.getId());
        entity.setProductId(dataObject.getProductId());
        entity.setName(dataObject.getName());
        entity.setApiKeyEncrypted(dataObject.getApiKeyEncrypted());
        entity.setWeight(dataObject.getWeight());
        entity.setPriority(dataObject.getPriority());
        entity.setState(ProductApiKeyState.fromCode(dataObject.getState()));
        if (dataObject.getLastUsedAt() != null) {
            entity.setLastUsedAt(dataObject.getLastUsedAt().atZone(ZoneOffset.UTC).toInstant());
        }
        return entity;
    }

    private ProductApiKeyDo toDataObject(ProductApiKey entity) {
        ProductApiKeyDo dataObject = new ProductApiKeyDo();
        dataObject.setId(entity.getId());
        dataObject.setProductId(entity.getProductId());
        dataObject.setName(entity.getName());
        dataObject.setApiKeyEncrypted(entity.getApiKeyEncrypted());
        dataObject.setWeight(entity.getWeight());
        dataObject.setPriority(entity.getPriority());
        dataObject.setState(entity.getState().getCode());
        if (entity.getLastUsedAt() != null) {
            dataObject.setLastUsedAt(LocalDateTime.ofInstant(entity.getLastUsedAt(), ZoneOffset.UTC));
        }
        return dataObject;
    }
}