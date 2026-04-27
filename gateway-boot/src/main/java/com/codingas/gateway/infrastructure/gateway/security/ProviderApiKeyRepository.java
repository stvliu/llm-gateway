package com.codingas.gateway.infrastructure.gateway.security;

import com.codingas.gateway.domain.security.entity.ProviderApiKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 提供商 API 密钥仓储接口
 */
@Repository
public interface ProviderApiKeyRepository extends JpaRepository<ProviderApiKey, Long> {

    /**
     * 根据密钥编码查找
     */
    Optional<ProviderApiKey> findByKeyCode(String keyCode);
}