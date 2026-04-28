package com.codingas.gateway.infrastructure.gateway.security;

import com.codingas.gateway.domain.security.entity.TokenLimit;
import com.codingas.gateway.domain.security.gateway.TokenLimitGateway;
import com.codingas.gateway.domain.security.repository.TokenLimitRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Token 限额网关实现
 *
 * <p>实现 TokenLimitGateway 接口，使用 JPA 进行持久化。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JpaTokenLimitGateway implements TokenLimitGateway {

    private final TokenLimitRepository tokenLimitRepository;

    @Override
    public TokenLimit save(TokenLimit tokenLimit) {
        return tokenLimitRepository.save(tokenLimit);
    }

    @Override
    public Optional<TokenLimit> findById(Long id) {
        return tokenLimitRepository.findById(id);
    }

    @Override
    public Optional<TokenLimit> findByLimitCode(String limitCode) {
        return tokenLimitRepository.findByLimitCode(limitCode);
    }

    @Override
    public List<TokenLimit> findByUserId(Long userId) {
        return tokenLimitRepository.findByUserId(userId);
    }

    @Override
    public List<TokenLimit> findAll() {
        return tokenLimitRepository.findAll();
    }

    @Override
    public long count() {
        return tokenLimitRepository.count();
    }

    @Override
    public void delete(TokenLimit tokenLimit) {
        tokenLimitRepository.delete(tokenLimit);
    }

    @Override
    public boolean existsByLimitCode(String limitCode) {
        return tokenLimitRepository.existsByLimitCode(limitCode);
    }

    @Override
    public void deductUsage(Long userId, Long inputTokens, Long outputTokens) {
        tokenLimitRepository.findAll().stream()
                .filter(t -> t.getUser() != null && t.getUser().getId().equals(userId))
                .findFirst()
                .ifPresent(t -> {
                    BigDecimal currentUsed = t.getUsedTokens() != null ? t.getUsedTokens() : BigDecimal.ZERO;
                    t.setUsedTokens(currentUsed.add(BigDecimal.valueOf(inputTokens + outputTokens)));
                    tokenLimitRepository.save(t);
                });
    }
}
