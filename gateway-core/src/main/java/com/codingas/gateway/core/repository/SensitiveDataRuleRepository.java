package com.codingas.gateway.core.repository;

import com.codingas.gateway.core.domain.entity.SensitiveDataRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SensitiveDataRuleRepository extends JpaRepository<SensitiveDataRule, Long> {

    Optional<SensitiveDataRule> findByRuleCode(String ruleCode);

    List<SensitiveDataRule> findByEnabledTrue();

    List<SensitiveDataRule> findByDataType(String dataType);

    boolean existsByRuleCode(String ruleCode);
}