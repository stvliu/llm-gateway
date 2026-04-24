package com.codingas.gateway.core.repository;

import com.codingas.gateway.core.domain.entity.RateLimitConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RateLimitConfigRepository extends JpaRepository<RateLimitConfig, Long> {

    Optional<RateLimitConfig> findByConfigCode(String configCode);

    boolean existsByConfigCode(String configCode);
}