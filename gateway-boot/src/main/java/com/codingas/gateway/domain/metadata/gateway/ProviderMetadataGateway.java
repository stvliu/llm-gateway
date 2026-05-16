package com.codingas.gateway.domain.metadata.gateway;

import com.codingas.gateway.domain.metadata.entity.ProviderMetadata;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

/**
 * 供应商元数据网关接口
 */
public interface ProviderMetadataGateway {

    /**
     * 保存供应商元数据
     */
    ProviderMetadata save(ProviderMetadata metadata);

    /**
     * 根据 ID 查询
     */
    Optional<ProviderMetadata> findById(Long id);

    /**
     * 根据 provider_id 查询
     */
    Optional<ProviderMetadata> findByProviderId(String providerId);

    /**
     * 分页条件查询
     */
    Page<ProviderMetadata> findByConditions(
        String providerType,
        String keyword,
        Pageable pageable
    );

    /**
     * 查询所有元数据
     */
    List<ProviderMetadata> findAllMetadata();

    /**
     * 检查 provider_id 是否存在
     */
    boolean existsByProviderId(String providerId);

    /**
     * 删除供应商元数据（物理删除）
     */
    void deleteById(Long id);
}
