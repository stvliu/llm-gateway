package com.codingas.gateway.infrastructure.metadata.database;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 供应商元数据 JPA Repository
 */
@Repository
public interface ProviderMetadataRepository extends
        JpaRepository<ProviderMetadataDo, Long>,
        JpaSpecificationExecutor<ProviderMetadataDo> {

    Optional<ProviderMetadataDo> findByProviderId(String providerId);

    boolean existsByProviderId(String providerId);
}
