package com.codingas.gateway.infrastructure.model.gateway.database;

import com.codingas.gateway.infrastructure.model.gateway.database.dataobject.ProviderApiKeyDo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 提供商 API 密钥仓储接口
 */
@Repository
public interface ProviderApiKeyRepository extends JpaRepository<ProviderApiKeyDo, Long> {

    /**
     * 根据提供商 ID 查找
     */
    Optional<ProviderApiKeyDo> findByProviderId(Long providerId);
}
