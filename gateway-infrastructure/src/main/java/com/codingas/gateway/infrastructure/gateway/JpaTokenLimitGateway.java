package com.codingas.gateway.core.infrastructure.gateway;

import com.codingas.gateway.core.domain.entity.TokenLimit;
import com.codingas.gateway.core.domain.gateway.TokenLimitGateway;
import com.codingas.gateway.core.repository.TokenLimitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Token 限额网关实现
 *
 * <p>实现 TokenLimitGateway 接口，使用 JPA 进行持久化。</p>
 */
@Component
@RequiredArgsConstructor
public class JpaTokenLimitGateway implements TokenLimitGateway {

    private final TokenLimitRepository repository;

    @Override
    public Optional<TokenLimit> findByUserId(Long userId) {
        return repository.findByUserId(userId).stream().findFirst();
    }

    @Override
    public Optional<TokenLimit> findByLimitCode(String limitCode) {
        return repository.findByLimitCode(limitCode);
    }

    @Override
    public Optional<TokenLimit> findByModelId(Long modelId) {
        return repository.findAll().stream()
                .filter(tl -> tl.getModelId() != null && tl.getModelId().equals(modelId))
                .findFirst();
    }

    @Override
    public TokenLimit save(TokenLimit tokenLimit) {
        return repository.save(tokenLimit);
    }

    @Override
    public void updateUsedTokens(String limitCode, BigDecimal usedTokens) {
        repository.findByLimitCode(limitCode).ifPresent(tl -> {
            tl.setUsedTokens(usedTokens);
            repository.save(tl);
        });
    }

    @Override
    public void addUsedTokens(String limitCode, BigDecimal tokens) {
        repository.findByLimitCode(limitCode).ifPresent(tl -> {
            tl.setUsedTokens(tl.getUsedTokens().add(tokens));
            repository.save(tl);
        });
    }
}
