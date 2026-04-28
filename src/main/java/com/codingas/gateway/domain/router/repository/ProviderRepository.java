package com.codingas.gateway.domain.router.repository;

import com.codingas.gateway.domain.router.entity.Provider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProviderRepository extends JpaRepository<Provider, Long> {
    Optional<Provider> findByProviderCode(String providerCode);
    List<Provider> findAllActive();
    List<Provider> findByEnabled(Boolean enabled);
    boolean existsByProviderCode(String providerCode);
}
