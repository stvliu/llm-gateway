package com.codingas.gateway.infrastructure.model.gateway.database;

import com.codingas.gateway.infrastructure.model.gateway.database.dataobject.ProviderDo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProviderRepository extends JpaRepository<ProviderDo, Long> {
    Optional<ProviderDo> findByProviderCode(String providerCode);
    List<ProviderDo> findAllActive();
    List<ProviderDo> findByEnabled(Boolean enabled);
    boolean existsByProviderCode(String providerCode);
}
