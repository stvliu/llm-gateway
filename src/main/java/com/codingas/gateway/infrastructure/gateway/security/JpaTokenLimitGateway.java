package com.codingas.gateway.infrastructure.gateway.security;

import com.codingas.gateway.domain.security.entity.TokenLimit;
import com.codingas.gateway.domain.security.gateway.TokenLimitGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

/**
 * Token 限额网关实现
 *
 * <p>实现 TokenLimitGateway 接口，使用 JPA 进行持久化。</p>
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class JpaTokenLimitGateway implements TokenLimitGateway {

    private final TokenLimitRepository repository;

    @Override
    public TokenLimit findByUserId(Long userId) {
        return repository.findAll().stream()
                .filter(t -> t.getUser() != null && t.getUser().getId().equals(userId))
                .findFirst()
                .orElse(null);
    }

    @Override
    public TokenLimit save(TokenLimit tokenLimit) {
        return repository.save(tokenLimit);
    }

    @Override
    public void deductUsage(Long userId, Long inputTokens, Long outputTokens) {
        repository.findAll().stream()
                .filter(t -> t.getUser() != null && t.getUser().getId().equals(userId))
                .findFirst()
                .ifPresent(t -> {
                    BigDecimal currentUsed = t.getUsedTokens() != null ? t.getUsedTokens() : BigDecimal.ZERO;
                    t.setUsedTokens(currentUsed.add(BigDecimal.valueOf(inputTokens + outputTokens)));
                    repository.save(t);
                });
    }
}

/**
 * Token 限额仓储接口
 */
interface TokenLimitRepository {
    List<TokenLimit> findAll();
    TokenLimit save(TokenLimit tokenLimit);
}
