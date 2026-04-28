package com.codingas.gateway.infrastructure.security;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface GatewayApiKeyRepository extends JpaRepository<GatewayApiKeyDo, Long> {
    Optional<GatewayApiKeyDo> findByKeyHash(String keyHash);
    Optional<GatewayApiKeyDo> findByKeyCode(String keyCode);
    List<GatewayApiKeyDo> findByUserId(Long userId);
    Page<GatewayApiKeyDo> findExpiringKeys(Instant now, Instant threshold, Pageable pageable);
    boolean existsByKeyCode(String keyCode);
}
