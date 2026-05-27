package com.codingas.gateway.infrastructure.supply.gateway.database.repository;

import com.codingas.gateway.infrastructure.supply.gateway.database.dataobject.ModelSpecDo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 模型规格 Repository
 */
public interface ModelSpecRepository extends JpaRepository<ModelSpecDo, Long> {

    Optional<ModelSpecDo> findByProviderModelId(String providerModelId);

    List<ModelSpecDo> findByState(String state);

    List<ModelSpecDo> findByIdIn(List<Long> ids);

    List<ModelSpecDo> findByProviderModelIdAndState(String providerModelId, String state);
}