package com.codingas.gateway.infrastructure.supply.gateway.database.repository;

import com.codingas.gateway.infrastructure.supply.gateway.database.dataobject.ProviderDo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 供应商 Repository
 */
public interface ProviderRepository extends JpaRepository<ProviderDo, Long> {

    Optional<ProviderDo> findByCode(String code);

    Optional<ProviderDo> findByName(String name);

    boolean existsByCode(String code);

    boolean existsByName(String name);

    List<ProviderDo> findByCodeContainingOrNameContaining(String codeKeyword, String nameKeyword);
}