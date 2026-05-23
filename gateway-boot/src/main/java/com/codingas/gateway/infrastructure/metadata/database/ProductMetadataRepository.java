package com.codingas.gateway.infrastructure.metadata.database;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 产品元数据 JPA Repository
 */
@Repository
public interface ProductMetadataRepository extends
        JpaRepository<ProductMetadataDo, Long>,
        JpaSpecificationExecutor<ProductMetadataDo> {

    List<ProductMetadataDo> findByProviderId(String providerId);

    Optional<ProductMetadataDo> findByProviderIdAndProductName(String providerId, String productName);

    Optional<ProductMetadataDo> findByProviderIdAndIsDefaultTrue(String providerId);

    boolean existsByProviderIdAndProductName(String providerId, String productName);
}
