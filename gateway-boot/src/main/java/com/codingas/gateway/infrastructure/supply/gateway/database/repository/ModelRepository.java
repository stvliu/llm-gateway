package com.codingas.gateway.infrastructure.supply.gateway.database.repository;

import com.codingas.gateway.infrastructure.supply.gateway.database.dataobject.ModelDo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 模型 Repository
 */
public interface ModelRepository extends JpaRepository<ModelDo, Long> {

    Optional<ModelDo> findByModelName(String modelName);

    List<ModelDo> findByState(String state);

    List<ModelDo> findByIdIn(List<Long> ids);

    List<ModelDo> findByModelNameAndState(String modelName, String state);
}