package com.codingas.gateway.domain.metadata.service;

import com.codingas.gateway.domain.metadata.entity.ProviderMetadata;
import org.springframework.stereotype.Service;

/**
 * 供应商元数据领域服务
 * <p>
 * 封装 ProviderMetadata 的业务逻辑，保持实体仅含 Getter/Setter。
 * </p>
 */
@Service
public class ProviderMetadataDomainService {

    /**
     * 逻辑删除
     */
    public void markDeleted(ProviderMetadata metadata, Long operatorId) {
        metadata.setUpdatedBy(operatorId);
        // updatedAt 由 JPA 审计自动设置
    }
}
