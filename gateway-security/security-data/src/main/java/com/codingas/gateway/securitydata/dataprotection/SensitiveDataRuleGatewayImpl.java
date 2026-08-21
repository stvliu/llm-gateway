/*
 * Copyright © 2025-2026 codingas.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.codingas.gateway.securitydata.dataprotection;

import com.codingas.gateway.security.dataprotection.SensitiveDataRule;
import com.codingas.gateway.security.dataprotection.SensitiveDataRuleGateway;
import com.codingas.gateway.securitydata.dataprotection.SensitiveDataRuleRepository;
import com.codingas.gateway.securitydata.dataprotection.SensitiveDataRuleDo;
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
