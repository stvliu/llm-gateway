package com.codingas.gateway.infrastructure.product.gateway.database.repository;

import com.codingas.gateway.infrastructure.product.gateway.database.dataobject.ProductDo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
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

    boolean existsByProviderIdAndName(Long providerId, String name);
}