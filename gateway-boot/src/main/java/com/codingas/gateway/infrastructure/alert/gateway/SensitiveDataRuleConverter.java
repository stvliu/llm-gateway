package com.codingas.gateway.infrastructure.alert.gateway;

import com.codingas.gateway.domain.dataprotection.entity.SensitiveDataRule;
import com.codingas.gateway.infrastructure.alert.gateway.database.dataobject.SensitiveDataRuleDo;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 敏感数据规则对象转换器
 *
 * <p>负责在领域实体 {@link SensitiveDataRule} 和数据对象 {@link SensitiveDataRuleDo} 之间进行转换。</p>
 */
@Component
public class SensitiveDataRuleConverter {

    /**
     * 将数据对象转换为领域实体
     *
     * @param ruleDo 数据对象
     * @return 领域实体
     */
    public SensitiveDataRule toDomain(SensitiveDataRuleDo ruleDo) {
        if (ruleDo == null) {
            return null;
        }

        SensitiveDataRule rule = new SensitiveDataRule();
        rule.setId(ruleDo.getId());
        rule.setRuleName(ruleDo.getRuleName());
        rule.setDataType(ruleDo.getDataType());
        rule.setRegexPattern(ruleDo.getRegexPattern());
        rule.setMaskFormat(ruleDo.getMaskFormat());
        rule.setEnabled(ruleDo.getEnabled());
        rule.setCreatedAt(ruleDo.getCreatedAt());
        rule.setUpdatedAt(ruleDo.getUpdatedAt());

        return rule;
    }

    /**
     * 将领域实体转换为数据对象
     *
     * @param rule 领域实体
     * @return 数据对象
     */
    public SensitiveDataRuleDo toDataObject(SensitiveDataRule rule) {
        if (rule == null) {
            return null;
        }

        SensitiveDataRuleDo ruleDo = new SensitiveDataRuleDo();
        ruleDo.setId(rule.getId());
        ruleDo.setRuleName(rule.getRuleName());
        ruleDo.setDataType(rule.getDataType());
        ruleDo.setRegexPattern(rule.getRegexPattern());
        ruleDo.setMaskFormat(rule.getMaskFormat());
        ruleDo.setEnabled(rule.getEnabled());
        ruleDo.setCreatedAt(rule.getCreatedAt());
        ruleDo.setUpdatedAt(rule.getUpdatedAt());

        return ruleDo;
    }

    /**
     * 将数据对象列表转换为领域实体列表
     *
     * @param ruleDos 数据对象列表
     * @return 领域实体列表
     */
    public List<SensitiveDataRule> toDomainList(List<SensitiveDataRuleDo> ruleDos) {
        if (ruleDos == null) {
            return List.of();
        }

        return ruleDos.stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    /**
     * 将领域实体列表转换为数据对象列表
     *
     * @param rules 领域实体列表
     * @return 数据对象列表
     */
    public List<SensitiveDataRuleDo> toDataObjectList(List<SensitiveDataRule> rules) {
        if (rules == null) {
            return List.of();
        }

        return rules.stream()
                .map(this::toDataObject)
                .collect(Collectors.toList());
    }

    /**
     * 将 Optional 数据对象转换为 Optional 领域实体
     *
     * @param ruleDoOptional 可选的数据对象
     * @return 可选的领域实体
     */
    public Optional<SensitiveDataRule> toDomainOptional(Optional<SensitiveDataRuleDo> ruleDoOptional) {
        return ruleDoOptional.map(this::toDomain);
    }
}
