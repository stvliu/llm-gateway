package com.codingas.gateway.core.repository;

import com.codingas.gateway.core.domain.entity.GatewayApiKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * GatewayApiKey 仓储接口
 */
@Repository
public interface GatewayApiKeyRepository extends JpaRepository<GatewayApiKey, Long> {

    /**
     * 根据 Key 哈希查询
     */
    Optional<GatewayApiKey> findByKeyHash(String keyHash);

    /**
     * 根据用户 ID 查询所有 Key
     */
    List<GatewayApiKey> findByUserId(Long userId);

    /**
     * 根据用户 ID 和 Provider ID 查询
     */
    List<GatewayApiKey> findByUserIdAndProviderId(Long userId, Long providerId);

    /**
     * 根据 Key 编码查询
     */
    Optional<GatewayApiKey> findByKeyCode(String keyCode);
}
