package com.codingas.gateway.domain.product.gateway;

import com.codingas.gateway.domain.product.entity.Product;
import com.codingas.gateway.domain.product.enums.ProductType;

import java.util.List;
import java.util.Optional;

/**
 * 产品 Gateway 接口
 */
public interface ProductGateway {

    Product save(Product product);

    Optional<Product> findById(Long id);

    List<Product> findByProviderId(Long providerId);

    List<Product> findByProviderIdAndType(Long providerId, ProductType type);

    List<Product> findByModel(String modelName);

    List<Product> findAllActive();

    void deleteById(Long id);

    boolean existsByProviderIdAndName(Long providerId, String name);

    /** 批量查找产品 */
    List<Product> findByIds(List<Long> ids);
}