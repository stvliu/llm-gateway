package com.codingas.gateway.infrastructure.alert.gateway.database;

import com.codingas.gateway.infrastructure.alert.gateway.database.dataobject.SensitiveDataRuleDo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 敏感数据规则仓储接口
 */
@Repository
public interface SensitiveDataRuleRepository extends JpaRepository<SensitiveDataRuleDo, Long> {
    
    Optional<SensitiveDataRuleDo> findByRuleName(String ruleName);
    
    List<SensitiveDataRuleDo> findByEnabled(Boolean enabled);
    
    List<SensitiveDataRuleDo> findByDataType(String dataType);
    
    boolean existsByRuleName(String ruleName);
    
    long count();

    SensitiveDataRuleDo save(SensitiveDataRuleDo rule);
}
