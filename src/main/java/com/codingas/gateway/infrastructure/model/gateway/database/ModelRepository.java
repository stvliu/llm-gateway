package com.codingas.gateway.infrastructure.model.gateway.database;

import com.codingas.gateway.infrastructure.model.gateway.database.dataobject.ModelDo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ModelRepository extends JpaRepository<ModelDo, Long> {
    Optional<ModelDo> findByModelCode(String modelCode);
    List<ModelDo> findAllActive();
    List<ModelDo> findByProviderId(Long providerId);
    boolean existsByModelCode(String modelCode);
}
