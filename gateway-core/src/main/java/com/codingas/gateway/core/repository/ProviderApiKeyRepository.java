package com.codingas.gateway.core.repository;

import com.codingas.gateway.core.domain.entity.ProviderApiKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * ProviderApiKey 仓储接口
 */
@Repository
public interface ProviderApiKeyRepository extends JpaRepository<ProviderApiKey, Long> {

    /**
     * 根据 Provider ID 查询所有 Key
     */
    List<ProviderApiKey> findByProviderId(Long providerId);

    /**
     * 根据 Provider ID 和状态查询
     */
    List<ProviderApiKey> findByProviderIdAndStatus(Long providerId, ProviderApiKey.ProviderApiKeyStatus status);

    /**
     * 根据 Key 编码查询
     */
    Optional<ProviderApiKey> findByKeyCode(String keyCode);

    /**
     * 查找激活状态且优先级最高的 Key
     */
    Optional<ProviderApiKey> findFirstByProviderIdAndStatusOrderByPriorityDesc(
            Long providerId, ProviderApiKey.ProviderApiKeyStatus status);
}
