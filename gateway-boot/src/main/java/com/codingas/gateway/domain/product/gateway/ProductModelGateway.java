package com.codingas.gateway.domain.product.gateway;

import com.codingas.gateway.domain.product.entity.ProductModel;

import java.util.List;
import java.util.Optional;

/**
 * 产品-模型关联网关接口
 */
public interface ProductModelGateway {

    /**
     * 保存关联
     */
    ProductModel save(ProductModel association);

    /**
     * 根据 ID 查询
     */
    Optional<ProductModel> findById(Long id);

    /**
     * 查询某产品的所有模型关联
     */
    List<ProductModel> findByProductId(Long productId);

    /**
     * 查询某模型的所有产品关联
     */
    List<ProductModel> findByModelId(Long modelId);

    /**
     * 精确查找：(product_id, model_id)
     */
    Optional<ProductModel> findByProductIdAndModelId(Long productId, Long modelId);

    /**
     * 批量保存
     */
    List<ProductModel> saveAll(List<ProductModel> associations);

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