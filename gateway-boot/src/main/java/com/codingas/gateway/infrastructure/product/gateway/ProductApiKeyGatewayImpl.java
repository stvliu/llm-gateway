package com.codingas.gateway.infrastructure.product.gateway;

import com.codingas.gateway.domain.security.service.ApiKeyEncryptionDomainService;
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
 *
 * <p>加解密在基础设施层处理：save() 时加密明文 Key，toEntity() 时解密返回明文。</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ProductApiKeyGatewayImpl implements ProductApiKeyGateway {

    private final ProductApiKeyRepository productApiKeyRepository;
    private final ApiKeyEncryptionDomainService encryptionService;

    @Override
    public ProductApiKey save(ProductApiKey apiKey) {
        ProductApiKeyDo dataObject = toDataObject(apiKey);
        if (apiKey.getId() == null) {
            dataObject.setCreatedAt(LocalDateTime.now());

            // 创建时：从明文计算哈希和密文
            String plainKey = apiKey.getApiKeyPlain();
            if (plainKey != null && !plainKey.isBlank()) {
                dataObject.setApiKeyHash(encryptionService.hashKey(plainKey));
                dataObject.setApiKeyEncrypted(encryptionService.encrypt(plainKey));
                dataObject.setApiKeyPrefix(plainKey.substring(0, Math.min(8, plainKey.length())));
            }
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
    public List<ProductApiKey> findByProductId(Long productId) {
        return productApiKeyRepository.findByProductId(productId).stream()
                .map(this::toEntity)
                .toList();
    }

    @Override
    public List<ProductApiKey> findByProductIdAndState(Long productId, ProductApiKeyState state) {
        return productApiKeyRepository.findByProductIdAndState(productId, state.getCode()).stream()
                .map(this::toEntity)
                .toList();
    }

    @Override
    public List<ProductApiKey> findActiveByProductId(Long productId) {
        return productApiKeyRepository.findActiveByProductId(productId).stream()
                .map(this::toEntity)
                .toList();
    }

    @Override
    public Optional<ProductApiKey> findDefaultByProductId(Long productId) {
        return productApiKeyRepository.findDefaultByProductId(productId).map(this::toEntity);
    }

    @Override
    public void deleteById(Long id) {
        productApiKeyRepository.deleteById(id);
    }

    @Override
    public long countActiveByProductId(Long productId) {
        return productApiKeyRepository.countByProductIdAndState(productId, "active");
    }

    @Override
    public void updateLastUsedAt(Long id) {
        productApiKeyRepository.findById(id).ifPresent(dataObject -> {
            dataObject.setLastUsedAt(LocalDateTime.now());
            productApiKeyRepository.save(dataObject);
        });
    }

    private ProductApiKey toEntity(ProductApiKeyDo dataObject) {
        ProductApiKey entity = new ProductApiKey();
        entity.setId(dataObject.getId());
        entity.setProductId(dataObject.getProductId());
        entity.setApiKeyPrefix(dataObject.getApiKeyPrefix());
        entity.setName(dataObject.getName());
        entity.setDescription(dataObject.getDescription());
        entity.setWeight(dataObject.getWeight());
        entity.setPriority(dataObject.getPriority());
        entity.setState(ProductApiKeyState.fromCode(dataObject.getState()));

        // 解密返回明文 Key
        if (dataObject.getApiKeyEncrypted() != null && !dataObject.getApiKeyEncrypted().isBlank()) {
            try {
                entity.setApiKeyPlain(encryptionService.decrypt(dataObject.getApiKeyEncrypted()));
            } catch (Exception e) {
                log.warn("Failed to decrypt ProductApiKey: id={}, error={}", dataObject.getId(), e.getMessage());
                entity.setApiKeyPlain(null);
            }
        }

        if (dataObject.getLastUsedAt() != null) {
            entity.setLastUsedAt(dataObject.getLastUsedAt().atZone(ZoneOffset.UTC).toInstant());
        }
        if (dataObject.getCreatedAt() != null) {
            entity.setCreatedAt(dataObject.getCreatedAt().toInstant(ZoneOffset.UTC));
        }
        if (dataObject.getUpdatedAt() != null) {
            entity.setUpdatedAt(dataObject.getUpdatedAt().toInstant(ZoneOffset.UTC));
        }
        return entity;
    }

    private ProductApiKeyDo toDataObject(ProductApiKey entity) {
        ProductApiKeyDo dataObject = new ProductApiKeyDo();
        dataObject.setId(entity.getId());
        dataObject.setProductId(entity.getProductId());
        dataObject.setApiKeyPrefix(entity.getApiKeyPrefix());
        dataObject.setName(entity.getName());
        dataObject.setDescription(entity.getDescription());
        dataObject.setWeight(entity.getWeight());
        dataObject.setPriority(entity.getPriority());
        dataObject.setState(entity.getState().getCode());

        if (entity.getLastUsedAt() != null) {
            dataObject.setLastUsedAt(LocalDateTime.ofInstant(entity.getLastUsedAt(), ZoneOffset.UTC));
        }

        // 更新时：保留已有的 hash 和 encrypted
        if (entity.getId() != null) {
            productApiKeyRepository.findById(entity.getId()).ifPresent(existing -> {
                dataObject.setApiKeyHash(existing.getApiKeyHash());
                dataObject.setApiKeyEncrypted(existing.getApiKeyEncrypted());
            });
        }

        return dataObject;
    }
}