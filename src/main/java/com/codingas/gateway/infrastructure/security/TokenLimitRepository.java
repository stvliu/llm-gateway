package com.codingas.gateway.infrastructure.security;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TokenLimitRepository extends JpaRepository<TokenLimitDo, Long> {
    Optional<TokenLimitDo> findByLimitCode(String limitCode);
    List<TokenLimitDo> findByUserId(Long userId);
    boolean existsByLimitCode(String limitCode);
}
