package com.codingas.gateway.domain.metadata.gateway;

import com.codingas.gateway.domain.metadata.entity.MetadataSource;
import com.codingas.gateway.domain.metadata.entity.ModelMetadata;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

/**
 * 模型元数据网关接口
 */
public interface ModelMetadataGateway {

    /**
     * 保存模型元数据
     */
    ModelMetadata save(ModelMetadata metadata);

    /**
     * 根据 ID 查询
     */
    Optional<ModelMetadata> findById(Long id);

    /**
     * 查询某供应商的所有模型
     */
    List<ModelMetadata> findByProviderId(String providerId);

    /**
     * 精确查找：(provider_id, provider_model_id)
     */
    Optional<ModelMetadata> findByProviderIdAndModelId(String providerId, String providerModelId);

    /**
     * 按数据来源查询
     */
    List<ModelMetadata> findBySource(MetadataSource source);

    /**
     * 分页条件查询
     */
    Page<ModelMetadata> findByConditions(
        String providerId,
        String keyword,
        MetadataSource source,
        Pageable pageable
    );

    /**
     * 批量保存
     */
    List<ModelMetadata> saveAll(List<ModelMetadata> metadataList);

    /**
     * 删除
     */
    void deleteById(Long id);

    /**
     * 检查模型是否存在
     */
    boolean existsByProviderIdAndModelId(String providerId, String providerModelId);
}