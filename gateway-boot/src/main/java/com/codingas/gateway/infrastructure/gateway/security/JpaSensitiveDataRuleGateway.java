package com.codingas.gateway.infrastructure.gateway.security;

import com.codingas.gateway.domain.security.entity.SensitiveDataRule;
import com.codingas.gateway.domain.security.gateway.SensitiveDataRuleGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 敏感数据规则网关 JPA 实现
 *
 * <p>实现 SensitiveDataRuleGateway 接口，使用 JPA 进行持久化。</p>
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class JpaSensitiveDataRuleGateway implements SensitiveDataRuleGateway {

    private final SensitiveDataRuleRepository repository;

    @Override
    public Optional<SensitiveDataRule> findByRuleCode(String ruleCode) {
        return repository.findAll().stream()
            .filter(r -> ruleCode.equals(r.getRuleName()))
            .findFirst();
    }

    @Override
    public List<SensitiveDataRule> findByEnabledTrue() {
        return repository.findAll().stream()
            .filter(r -> Boolean.TRUE.equals(r.getEnabled()))
            .toList();
    }

    @Override
    public List<SensitiveDataRule> findByDataType(String dataType) {
        return repository.findAll().stream()
            .filter(r -> dataType.equals(r.getDataType()))
            .toList();
    }

    @Override
    public boolean existsByRuleCode(String ruleCode) {
        return repository.findAll().stream()
            .anyMatch(r -> ruleCode.equals(r.getRuleName()));
    }

    @Override
    public long count() {
        return repository.count();
    }

    @Override
    public List<SensitiveDataRule> saveAll(List<SensitiveDataRule> rules) {
        List<SensitiveDataRule> saved = new ArrayList<>();
        for (SensitiveDataRule rule : rules) {
            saved.add(repository.save(rule));
        }
        return saved;
    }

    @Override
    public SensitiveDataRule save(SensitiveDataRule rule) {
        return repository.save(rule);
    }
}

/**
 * 敏感数据规则仓储接口
 */
interface SensitiveDataRuleRepository {
    List<SensitiveDataRule> findAll();
    long count();
    SensitiveDataRule save(SensitiveDataRule rule);
}
