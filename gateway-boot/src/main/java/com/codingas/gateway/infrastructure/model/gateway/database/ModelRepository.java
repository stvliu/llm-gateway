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

    /**
     * 查找同名模型的所有记录（多渠道）
     *
     * @param providerModelId 提供商模型 ID
     * @return 所有渠道列表，按 priority 升序排序
     */
    @Query("SELECT m FROM ModelDo m LEFT JOIN FETCH m.provider " +
           "WHERE m.providerModelId = :providerModelId ORDER BY m.priority ASC")
    List<ModelDo> findAllByProviderModelId(@Param("providerModelId") String providerModelId);

    /**
     * 查找同名模型的活跃记录
     *
     * @param providerModelId 提供商模型 ID
     * @return 活跃渠道列表，按 priority 升序排序
     */
    @Query("SELECT m FROM ModelDo m LEFT JOIN FETCH m.provider " +
           "WHERE m.providerModelId = :providerModelId AND m.state = 'ACTIVE' ORDER BY m.priority ASC")
    List<ModelDo> findActiveByProviderModelId(@Param("providerModelId") String providerModelId);
}