package com.codingas.gateway.application.product;

import com.codingas.gateway.application.product.dto.ProductRequest;
import com.codingas.gateway.application.product.dto.ProductResponse;
import com.codingas.gateway.domain.product.entity.Product;
import com.codingas.gateway.domain.product.enums.ProductType;
import com.codingas.gateway.domain.product.exception.ProductNotFoundException;
import com.codingas.gateway.domain.product.gateway.ProductGateway;
import com.codingas.gateway.domain.model.gateway.ProviderGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 产品应用服务实现
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProductServiceImpl implements ProductService {

    private final ProductGateway productGateway;
    private final ProviderGateway providerGateway;

    @Override
    @Transactional
    public ProductResponse create(ProductRequest request) {
        if (productGateway.existsByProviderIdAndName(request.getProviderId(), request.getName())) {
            throw new IllegalArgumentException("产品名称已存在: " + request.getName());
        }

        Product product = new Product();
        product.setProviderId(request.getProviderId());
        product.setName(request.getName());
        product.setProductType(ProductType.fromCode(request.getProductType()));
        product.setModels(request.getModels());
        product.setEndpoints(request.getEndpoints());
        product.setQuotaLimit(request.getQuotaLimit());

        providerGateway.findById(request.getProviderId())
            .ifPresent(p -> product.setProviderName(p.getName()));

        Product saved = productGateway.save(product);
        log.info("Created product: id={}, name={}", saved.getId(), saved.getName());

        return toResponse(saved);
    }

    @Override
    @Transactional
    public ProductResponse update(Long id, ProductRequest request) {
        Product product = productGateway.findById(id)
            .orElseThrow(() -> new ProductNotFoundException(id));

        if (!product.getName().equals(request.getName())) {
            if (productGateway.existsByProviderIdAndName(request.getProviderId(), request.getName())) {
                throw new IllegalArgumentException("产品名称已存在: " + request.getName());
            }
        }

        product.setProviderId(request.getProviderId());
        product.setName(request.getName());
        product.setProductType(ProductType.fromCode(request.getProductType()));
        product.setModels(request.getModels());
        product.setEndpoints(request.getEndpoints());
        product.setQuotaLimit(request.getQuotaLimit());

        Product saved = productGateway.save(product);
        log.info("Updated product: id={}", saved.getId());

        return toResponse(saved);
    }

    @Override
    public ProductResponse getById(Long id) {
        Product product = productGateway.findById(id)
            .orElseThrow(() -> new ProductNotFoundException(id));
        return toResponse(product);
    }

    @Override
    public List<ProductResponse> getByProviderId(Long providerId) {
        return productGateway.findByProviderId(providerId).stream()
            .map(this::toResponse)
            .toList();
    }

    @Override
    public List<ProductResponse> getByProviderIdAndType(Long providerId, ProductType type) {
        return productGateway.findByProviderIdAndType(providerId, type).stream()
            .map(this::toResponse)
            .toList();
    }

    @Override
    @Transactional
    public void delete(Long id) {
        productGateway.deleteById(id);
        log.info("Deleted product: id={}", id);
    }

    private ProductResponse toResponse(Product product) {
        ProductResponse response = new ProductResponse();
        response.setId(product.getId());
        response.setProviderId(product.getProviderId());
        response.setProviderName(product.getProviderName());
        response.setName(product.getName());
        response.setProductType(product.getProductType().getCode());
        response.setModels(product.getModels());
        response.setEndpoints(product.getEndpoints());
        response.setQuotaLimit(product.getQuotaLimit());
        response.setState(product.getState().getCode());
        return response;
    }
}
