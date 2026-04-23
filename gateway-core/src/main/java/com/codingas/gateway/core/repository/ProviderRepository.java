package com.codingas.gateway.core.repository;

import com.codingas.gateway.core.domain.entity.Provider;
import com.codingas.gateway.core.domain.enums.ProviderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Provider 仓储接口
 */
@Repository
public interface ProviderRepository extends JpaRepository<Provider, Long> {

    /**
     * 根据 Provider 编码查询
     */
    Optional<Provider> findByProviderCode(String providerCode);

    /**
     * 根据状态查询 Provider
     */
    List<Provider> findByStatus(ProviderStatus status);

    /**
     * 检查 Provider 编码是否存在
     */
    boolean existsByProviderCode(String providerCode);
}
