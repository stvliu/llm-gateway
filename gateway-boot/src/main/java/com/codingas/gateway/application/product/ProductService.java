package com.codingas.gateway.application.product;

import com.codingas.gateway.application.product.dto.ProductRequest;
import com.codingas.gateway.application.product.dto.ProductResponse;
import com.codingas.gateway.domain.product.enums.ProductType;

import java.util.List;

/**
 * 产品应用服务接口
 */
public interface ProductService {

    ProductResponse create(ProductRequest request);

    ProductResponse update(Long id, ProductRequest request);

    ProductResponse getById(Long id);

    List<ProductResponse> getByProviderId(Long providerId);

    List<ProductResponse> getByProviderIdAndType(Long providerId, ProductType type);

    void delete(Long id);
}
