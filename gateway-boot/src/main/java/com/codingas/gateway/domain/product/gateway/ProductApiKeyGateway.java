package com.codingas.gateway.domain.product.gateway;

import com.codingas.gateway.domain.product.entity.ProductApiKey;

import java.util.List;
import java.util.Optional;

/**
 * 产品 API Key Gateway 接口
 */
public interface ProductApiKeyGateway {

    ProductApiKey save(ProductApiKey apiKey);

    Optional<ProductApiKey> findById(Long id);

    List<ProductApiKey> findActiveByProductId(Long productId);

    Optional<ProductApiKey> findDefaultByProductId(Long productId);

    void updateLastUsedAt(Long id);

    void deleteById(Long id);

    long countActiveByProductId(Long productId);
}