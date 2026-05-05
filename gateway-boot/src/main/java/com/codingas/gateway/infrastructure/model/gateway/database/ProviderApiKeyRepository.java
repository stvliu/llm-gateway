package com.codingas.gateway.infrastructure.model.gateway.database;

import com.codingas.gateway.infrastructure.model.gateway.database.dataobject.ProviderApiKeyDo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
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

    /**
     * 获取最大版本号
     *
     * @return 最大版本号，无数据返回 null
     */
    @Query("SELECT MAX(k.version) FROM ProviderApiKeyDo k")
    Long findMaxVersion();
}
