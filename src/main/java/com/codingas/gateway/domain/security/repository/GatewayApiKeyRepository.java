package com.codingas.gateway.domain.security.repository;

import com.codingas.gateway.domain.security.entity.GatewayApiKey;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface GatewayApiKeyRepository extends JpaRepository<GatewayApiKey, Long> {
    Optional<GatewayApiKey> findByKeyHash(String keyHash);
    Optional<GatewayApiKey> findByKeyCode(String keyCode);
    List<GatewayApiKey> findByUserId(Long userId);
    Page<GatewayApiKey> findExpiringKeys(Instant now, Instant threshold, Pageable pageable);
    boolean existsByKeyCode(String keyCode);
}
