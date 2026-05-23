package com.codingas.gateway.infrastructure.team.gateway.database.repository;

import com.codingas.gateway.infrastructure.team.gateway.database.dataobject.UserApiKeyDo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * 用户 API Key JPA Repository
 */
public interface UserApiKeyRepository extends JpaRepository<UserApiKeyDo, Long> {

    List<UserApiKeyDo> findByUserId(Long userId);

    Optional<UserApiKeyDo> findByKeyPrefix(String keyPrefix);

    /** 查询关联某产品的 Key ID 列表 */
    @Query("SELECT DISTINCT u.id FROM UserApiKeyDo u JOIN u.productIds p WHERE p = :productId")
    List<Long> findIdsByProductId(@Param("productId") Long productId);
}