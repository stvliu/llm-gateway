package com.codingas.gateway.infrastructure.team.gateway.database.repository;

import com.codingas.gateway.infrastructure.team.gateway.database.dataobject.UserApiKeyProductDo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * UserApiKey-Product 关联 JPA Repository
 */
public interface UserApiKeyProductRepository extends JpaRepository<UserApiKeyProductDo, Long> {

    List<UserApiKeyProductDo> findByUserApiKeyId(Long userApiKeyId);

    @Query("SELECT DISTINCT p.productId FROM UserApiKeyProductDo p WHERE p.userApiKeyId = :userApiKeyId")
    List<Long> findProductIdByUserApiKeyId(@Param("userApiKeyId") Long userApiKeyId);

    @Query("SELECT DISTINCT p.userApiKeyId FROM UserApiKeyProductDo p WHERE p.productId = :productId")
    List<Long> findUserApiKeyIdByProductId(@Param("productId") Long productId);

    @Modifying
    @Query("DELETE FROM UserApiKeyProductDo p WHERE p.userApiKeyId = :userApiKeyId")
    void deleteByUserApiKeyId(@Param("userApiKeyId") Long userApiKeyId);
}