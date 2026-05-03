package com.codingas.gateway.infrastructure.quota.gateway.database;

import com.codingas.gateway.infrastructure.quota.gateway.database.dataobject.TokenLimitDo;
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
