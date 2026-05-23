package com.codingas.gateway.infrastructure.product.gateway.database.repository;

import com.codingas.gateway.infrastructure.product.gateway.database.dataobject.ProductModelDo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 产品-模型关联 Repository
 */
@Repository
public interface ProductModelRepository extends JpaRepository<ProductModelDo, Long> {

    List<ProductModelDo> findByProductId(Long productId);

    List<ProductModelDo> findByModelId(Long modelId);

    Optional<ProductModelDo> findByProductIdAndModelId(Long productId, Long modelId);

    void deleteByProductId(Long productId);

    void deleteByModelId(Long modelId);

    boolean existsByProductIdAndModelId(Long productId, Long modelId);
}