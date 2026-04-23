package com.codingas.gateway.core.repository;

import com.codingas.gateway.core.domain.entity.Model;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Model 仓储接口
 */
@Repository
public interface ModelRepository extends JpaRepository<Model, Long> {

    /**
     * 根据模型编码查询
     */
    Optional<Model> findByModelCode(String modelCode);

    /**
     * 根据 Provider ID 查询所有模型
     */
    List<Model> findByProviderId(Long providerId);

    /**
     * 根据 Provider ID 和 provider_model_id 查询
     */
    Optional<Model> findByProviderIdAndProviderModelId(Long providerId, String providerModelId);

    /**
     * 检查模型编码是否存在
     */
    boolean existsByModelCode(String modelCode);
}
