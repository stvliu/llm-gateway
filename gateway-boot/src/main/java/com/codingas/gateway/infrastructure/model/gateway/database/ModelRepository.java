package com.codingas.gateway.infrastructure.model.gateway.database;

import com.codingas.gateway.domain.model.enums.ModelState;
import com.codingas.gateway.infrastructure.model.gateway.database.dataobject.ModelDo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ModelRepository extends JpaRepository<ModelDo, Long> {

    @Query("SELECT m FROM ModelDo m LEFT JOIN FETCH m.provider WHERE m.providerModelId = :providerModelId")
    Optional<ModelDo> findByProviderModelId(@Param("providerModelId") String providerModelId);

    @Query("SELECT m FROM ModelDo m LEFT JOIN FETCH m.provider WHERE m.state = :state")
    List<ModelDo> findByState(@Param("state") ModelState state);

    default List<ModelDo> findActive() {
        return findByState(ModelState.ACTIVE);
    }

    @Query("SELECT m FROM ModelDo m LEFT JOIN FETCH m.provider WHERE m.provider.id = :providerId")
    List<ModelDo> findByProviderId(@Param("providerId") Long providerId);

    @Query("SELECT m FROM ModelDo m LEFT JOIN FETCH m.provider")
    List<ModelDo> findAllWithProvider();

    @Query("SELECT m FROM ModelDo m LEFT JOIN FETCH m.provider WHERE m.id = :id")
    Optional<ModelDo> findByIdWithProvider(@Param("id") Long id);
}