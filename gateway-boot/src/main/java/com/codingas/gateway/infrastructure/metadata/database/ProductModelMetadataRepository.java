package com.codingas.gateway.infrastructure.metadata.database;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 产品-模型元数据关联 JPA Repository
 */
@Repository
public interface ProductModelMetadataRepository extends JpaRepository<ProductModelMetadataDo, Long> {

    List<ProductModelMetadataDo> findByProductId(Long productId);

    List<ProductModelMetadataDo> findByModelId(Long modelId);

    Optional<ProductModelMetadataDo> findByProductIdAndModelId(Long productId, Long modelId);

    void deleteByProductId(Long productId);

    void deleteByModelId(Long modelId);

    boolean existsByProductIdAndModelId(Long productId, Long modelId);
}