package com.codingas.gateway.domain.product.gateway;

import com.codingas.gateway.domain.product.entity.ProductApiKey;
import com.codingas.gateway.domain.product.enums.ProductApiKeyState;

import java.util.List;
import java.util.Optional;

/**
 * 产品 API Key Gateway 接口
 */
public interface ProductApiKeyGateway {

    ProductApiKey save(ProductApiKey apiKey);

    Optional<ProductApiKey> findById(Long id);

    List<ProductApiKey> findActiveByProductId(Long productId);

    List<ProductApiKey> findByProductId(Long productId);

    List<ProductApiKey> findByProductIdAndState(Long productId, ProductApiKeyState state);

    Optional<ProductApiKey> findDefaultByProductId(Long productId);

    void updateLastUsedAt(Long id);

    void deleteById(Long id);

    long countActiveByProductId(Long productId);

    /**
     * 获取最大版本号
     */
    default long getMaxVersion() {
        return 0L;
    }
}