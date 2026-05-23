package com.codingas.gateway.domain.metadata.gateway;

import com.codingas.gateway.domain.metadata.entity.MetadataSource;
import com.codingas.gateway.domain.metadata.entity.ProductModelMetadata;

import java.util.List;
import java.util.Optional;

/**
 * 产品-模型元数据关联网关接口
 */
public interface ProductModelMetadataGateway {

    /**
     * 保存关联
     */
    ProductModelMetadata save(ProductModelMetadata association);

    /**
     * 根据 ID 查询
     */
    Optional<ProductModelMetadata> findById(Long id);

    /**
     * 查询某产品的所有模型关联
     */
    List<ProductModelMetadata> findByProductId(Long productId);

    /**
     * 查询某模型的所有产品关联
     */
    List<ProductModelMetadata> findByModelId(Long modelId);

    /**
     * 精确查找：(product_id, model_id)
     */
    Optional<ProductModelMetadata> findByProductIdAndModelId(Long productId, Long modelId);

    /**
     * 批量保存
     */
    List<ProductModelMetadata> saveAll(List<ProductModelMetadata> associations);

    /**
     * 删除
     */
    void deleteById(Long id);

    /**
     * 删除某产品的所有关联
     */
    void deleteByProductId(Long productId);

    /**
     * 删除某模型的所有关联
     */
    void deleteByModelId(Long modelId);

    /**
     * 检查关联是否存在
     */
    boolean existsByProductIdAndModelId(Long productId, Long modelId);
}