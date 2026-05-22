package com.codingas.gateway.domain.metadata.gateway;

import com.codingas.gateway.domain.metadata.entity.ProductMetadata;

import java.util.List;
import java.util.Optional;

/**
 * 产品元数据网关接口
 */
public interface ProductMetadataGateway {

    /**
     * 保存产品元数据
     */
    ProductMetadata save(ProductMetadata metadata);

    /**
     * 根据 ID 查询
     */
    Optional<ProductMetadata> findById(Long id);

    /**
     * 查询某供应商的所有产品
     */
    List<ProductMetadata> findByProviderId(String providerId);

    /**
     * 精确查找：(provider_id, product_name)
     */
    Optional<ProductMetadata> findByProviderIdAndProductName(String providerId, String productName);

    /**
     * 查询某供应商的默认产品
     */
    Optional<ProductMetadata> findDefaultByProviderId(String providerId);

    /**
     * 批量保存
     */
    List<ProductMetadata> saveAll(List<ProductMetadata> metadataList);

    /**
     * 删除
     */
    void deleteById(Long id);

    /**
     * 检查产品是否存在
     */
    boolean existsByProviderIdAndProductName(String providerId, String productName);
}
