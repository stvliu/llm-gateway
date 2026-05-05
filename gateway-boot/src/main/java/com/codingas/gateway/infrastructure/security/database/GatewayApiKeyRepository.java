package com.codingas.gateway.infrastructure.security.database;

import com.codingas.gateway.infrastructure.security.database.dataobject.GatewayApiKeyDo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface GatewayApiKeyRepository extends JpaRepository<GatewayApiKeyDo, Long> {

    @Query("SELECT k FROM GatewayApiKeyDo k LEFT JOIN FETCH k.user WHERE k.keyHash = :keyHash")
    Optional<GatewayApiKeyDo> findByKeyHash(@Param("keyHash") String keyHash);

    List<GatewayApiKeyDo> findByUserId(Long userId);

    @Query("SELECT k FROM GatewayApiKeyDo k WHERE k.expiresAt BETWEEN :now AND :threshold AND k.status = 'ACTIVE'")
    Page<GatewayApiKeyDo> findExpiringKeys(@Param("now") Instant now, @Param("threshold") Instant threshold, Pageable pageable);
}
