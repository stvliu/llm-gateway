package com.codingas.gateway.infrastructure.metadata.database;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 模型元数据 JPA Repository
 */
@Repository
public interface ModelMetadataRepository extends
        JpaRepository<ModelMetadataDo, Long>,
        JpaSpecificationExecutor<ModelMetadataDo> {

    List<ModelMetadataDo> findByProviderId(String providerId);

    Optional<ModelMetadataDo> findByProviderIdAndProviderModelId(String providerId, String providerModelId);

    boolean existsByProviderIdAndProviderModelId(String providerId, String providerModelId);

    List<ModelMetadataDo> findBySource(String source);
}
