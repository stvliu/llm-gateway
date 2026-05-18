package com.codingas.gateway.infrastructure.product.gateway.database.repository;

import com.codingas.gateway.infrastructure.product.gateway.database.dataobject.ProductDo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 产品 Repository
 */
@Repository
public interface ProductRepository extends JpaRepository<ProductDo, Long> {

    List<ProductDo> findByProviderId(Long providerId);

    List<ProductDo> findByProviderIdAndProductType(Long providerId, String productType);

    @Query("SELECT p FROM ProductDo p WHERE p.state = 'active'")
    List<ProductDo> findAllActive();

    @Query("SELECT p FROM ProductDo p WHERE :modelName MEMBER OF p.models AND p.state = 'active'")
    List<ProductDo> findByModel(@Param("modelName") String modelName);

    boolean existsByProviderIdAndName(Long providerId, String name);
}