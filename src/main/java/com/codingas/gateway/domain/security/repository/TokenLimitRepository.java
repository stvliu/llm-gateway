package com.codingas.gateway.domain.security.repository;

import com.codingas.gateway.domain.security.entity.TokenLimit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TokenLimitRepository extends JpaRepository<TokenLimit, Long> {
    Optional<TokenLimit> findByLimitCode(String limitCode);
    List<TokenLimit> findByUserId(Long userId);
    boolean existsByLimitCode(String limitCode);
}
