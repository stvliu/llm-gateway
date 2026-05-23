package com.codingas.gateway.infrastructure.product.gateway;

import com.codingas.gateway.domain.product.entity.Product;
import com.codingas.gateway.domain.product.enums.ProductState;
import com.codingas.gateway.domain.product.enums.ProductType;
import com.codingas.gateway.domain.product.gateway.ProductGateway;
import com.codingas.gateway.infrastructure.product.gateway.database.dataobject.ProductDo;
import com.codingas.gateway.infrastructure.product.gateway.database.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 产品 Gateway 实现
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ProductGatewayImpl implements ProductGateway {

    private final ProductRepository productRepository;

    @Override
    public Product save(Product product) {
        ProductDo dataObject = toDataObject(product);
        if (product.getId() == null) {
            dataObject.setCreatedAt(LocalDateTime.now());
        }
        dataObject.setUpdatedAt(LocalDateTime.now());
        ProductDo saved = productRepository.save(dataObject);
        return toEntity(saved);
    }

    @Override
    public Optional<Product> findById(Long id) {
        return productRepository.findById(id).map(this::toEntity);
    }

    @Override
    public List<Product> findByProviderId(Long providerId) {
        return productRepository.findByProviderId(providerId).stream()
            .map(this::toEntity)
            .toList();
    }

    @Override
    public List<Product> findByProviderIdAndType(Long providerId, ProductType type) {
        return productRepository.findByProviderIdAndProductType(providerId, type.getCode()).stream()
            .map(this::toEntity)
            .toList();
    }

    @Override
    public List<Product> findAllActive() {
        return productRepository.findAllActive().stream()
            .map(this::toEntity)
            .toList();
    }

    @Override
    public void deleteById(Long id) {
        productRepository.deleteById(id);
    }

    @Override
    public boolean existsByProviderIdAndName(Long providerId, String name) {
        return productRepository.existsByProviderIdAndName(providerId, name);
    }

    @Override
    public List<Product> findByIds(List<Long> ids) {
        return productRepository.findAllById(ids).stream()
                .map(this::toEntity)
                .toList();
    }

    private Product toEntity(ProductDo dataObject) {
        Product entity = new Product();
        entity.setId(dataObject.getId());
        entity.setProviderId(dataObject.getProviderId());
        entity.setName(dataObject.getName());
        entity.setProductType(ProductType.fromCode(dataObject.getProductType()));
        entity.setEndpoints(dataObject.getEndpoints());
        entity.setInputPrice(dataObject.getInputPrice());
        entity.setOutputPrice(dataObject.getOutputPrice());
        entity.setReasoningPrice(dataObject.getReasoningPrice());
        entity.setCacheReadPrice(dataObject.getCacheReadPrice());
        entity.setCacheWritePrice(dataObject.getCacheWritePrice());
        entity.setInputAudioPrice(dataObject.getInputAudioPrice());
        entity.setOutputAudioPrice(dataObject.getOutputAudioPrice());
        entity.setQuotaLimit(dataObject.getQuotaLimit());
        entity.setState(ProductState.fromCode(dataObject.getState()));
        return entity;
    }

    private ProductDo toDataObject(Product entity) {
        ProductDo dataObject = new ProductDo();
        dataObject.setId(entity.getId());
        dataObject.setProviderId(entity.getProviderId());
        dataObject.setName(entity.getName());
        dataObject.setProductType(entity.getProductType().getCode());
        dataObject.setEndpoints(entity.getEndpoints());
        dataObject.setInputPrice(entity.getInputPrice());
        dataObject.setOutputPrice(entity.getOutputPrice());
        dataObject.setReasoningPrice(entity.getReasoningPrice());
        dataObject.setCacheReadPrice(entity.getCacheReadPrice());
        dataObject.setCacheWritePrice(entity.getCacheWritePrice());
        dataObject.setInputAudioPrice(entity.getInputAudioPrice());
        dataObject.setOutputAudioPrice(entity.getOutputAudioPrice());
        dataObject.setQuotaLimit(entity.getQuotaLimit());
        dataObject.setState(entity.getState().getCode());
        return dataObject;
    }
}