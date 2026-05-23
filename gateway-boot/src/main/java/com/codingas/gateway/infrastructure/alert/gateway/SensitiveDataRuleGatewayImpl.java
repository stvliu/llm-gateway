package com.codingas.gateway.infrastructure.alert.gateway;

import com.codingas.gateway.domain.dataprotection.entity.SensitiveDataRule;
import com.codingas.gateway.domain.dataprotection.gateway.SensitiveDataRuleGateway;
import com.codingas.gateway.infrastructure.alert.gateway.database.SensitiveDataRuleRepository;
import com.codingas.gateway.infrastructure.alert.gateway.database.dataobject.SensitiveDataRuleDo;
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
public class SensitiveDataRuleGatewayImpl implements SensitiveDataRuleGateway {

    private final SensitiveDataRuleRepository repository;
    private final SensitiveDataRuleConverter converter;

    @Override
    public Optional<SensitiveDataRule> findByRuleCode(String ruleCode) {
        return repository.findByRuleName(ruleCode)
                .map(converter::toDomain);
    }

    @Override
    public List<SensitiveDataRule> findByEnabledTrue() {
        return converter.toDomainList(repository.findByEnabled(true));
    }

    @Override
    public List<SensitiveDataRule> findByDataType(String dataType) {
        return converter.toDomainList(repository.findByDataType(dataType));
    }

    @Override
    public boolean existsByRuleCode(String ruleCode) {
        return repository.existsByRuleName(ruleCode);
    }

    @Override
    public long count() {
        return repository.count();
    }

    @Override
    public List<SensitiveDataRule> saveAll(List<SensitiveDataRule> rules) {
        List<SensitiveDataRuleDo> ruleDos = converter.toDataObjectList(rules);
        List<SensitiveDataRuleDo> savedDos = new ArrayList<>();
        for (SensitiveDataRuleDo ruleDo : ruleDos) {
            savedDos.add(repository.save(ruleDo));
        }
        return converter.toDomainList(savedDos);
    }

    @Override
    public SensitiveDataRule save(SensitiveDataRule rule) {
        SensitiveDataRuleDo ruleDo = converter.toDataObject(rule);
        SensitiveDataRuleDo savedDo = repository.save(ruleDo);
        return converter.toDomain(savedDo);
    }
}
