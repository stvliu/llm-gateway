package com.codingas.gateway.infrastructure.security.database;

import com.codingas.gateway.domain.security.enums.GatewayApiKeyState;
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

    @Query("SELECT k FROM GatewayApiKeyDo k LEFT JOIN FETCH k.user WHERE k.user.id = :userId")
    List<GatewayApiKeyDo> findByUserId(@Param("userId") Long userId);

    @Query("SELECT k FROM GatewayApiKeyDo k LEFT JOIN FETCH k.user")
    List<GatewayApiKeyDo> findAllWithUser();

    @Query("SELECT k FROM GatewayApiKeyDo k WHERE k.expiresAt BETWEEN :now AND :threshold AND k.state = :state")
    Page<GatewayApiKeyDo> findExpiringKeysByState(@Param("now") Instant now, @Param("threshold") Instant threshold, @Param("state") GatewayApiKeyState state, Pageable pageable);

    default Page<GatewayApiKeyDo> findExpiringKeys(Instant now, Instant threshold, Pageable pageable) {
        return findExpiringKeysByState(now, threshold, GatewayApiKeyState.ACTIVE, pageable);
    }
}
