package com.codingas.gateway.infrastructure.model.gateway.database;

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

    @Query("SELECT m FROM ModelDo m LEFT JOIN FETCH m.provider WHERE m.status = :status")
    List<ModelDo> findByStatus(@Param("status") ModelDo.ModelStatus status);

    @Query("SELECT m FROM ModelDo m LEFT JOIN FETCH m.provider WHERE m.provider.id = :providerId")
    List<ModelDo> findByProviderId(@Param("providerId") Long providerId);

    /**
     * 获取最大版本号
     *
     * @return 最大版本号，无数据返回 null
     */
    @Query("SELECT MAX(m.version) FROM ModelDo m")
    Long findMaxVersion();
}
