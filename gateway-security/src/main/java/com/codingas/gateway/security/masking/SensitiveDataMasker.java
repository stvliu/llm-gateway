package com.codingas.gateway.security.masking;

import com.codingas.gateway.core.domain.entity.SensitiveDataRule;
import com.codingas.gateway.core.repository.SensitiveDataRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * 敏感数据脱敏服务
 *
 * <p>支持手机号、身份证、银行卡等敏感数据的自动检测和脱敏。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SensitiveDataMasker {

    private final SensitiveDataRuleRepository ruleRepository;

    // 本地缓存已启用的规则
    private Map<String, CompiledRule> compiledRules = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        loadRules();
    }

    /**
     * 加载脱敏规则
     */
    public void loadRules() {
        List<SensitiveDataRule> rules = ruleRepository.findByEnabledTrue();
        compiledRules.clear();

        for (SensitiveDataRule rule : rules) {
            try {
                CompiledRule compiled = new CompiledRule(
                    rule.getRuleCode(),
                    Pattern.compile(rule.getPattern()),
                    rule.getMaskFormat()
                );
                compiledRules.put(rule.getRuleCode(), compiled);
                log.info("Loaded sensitive data rule: {} -> {}", rule.getRuleCode(), rule.getMaskFormat());
            } catch (Exception e) {
                log.warn("Failed to compile rule {}: {}", rule.getRuleCode(), e.getMessage());
            }
        }

        log.info("Loaded {} sensitive data rules", compiledRules.size());
    }

    /**
     * 对文本进行脱敏处理
     *
     * @param text 原始文本
     * @return 脱敏后的文本
     */
    public String mask(String text) {
        if (text == null || text.isBlank()) {
            return text;
        }

        String masked = text;
        for (CompiledRule rule : compiledRules.values()) {
            masked = rule.pattern.matcher(masked).replaceAll(rule.maskFormat);
        }
        return masked;
    }

    /**
     * 对文本进行脱敏处理，但保留指定规则
     *
     * @param text 原始文本
     * @param excludeRules 不需要应用的规则代码
     * @return 脱敏后的文本
     */
    public String mask(String text, List<String> excludeRules) {
        if (text == null || text.isBlank()) {
            return text;
        }

        String masked = text;
        for (Map.Entry<String, CompiledRule> entry : compiledRules.entrySet()) {
            if (excludeRules != null && excludeRules.contains(entry.getKey())) {
                continue;
            }
            masked = entry.getValue().pattern.matcher(masked).replaceAll(entry.getValue().maskFormat);
        }
        return masked;
    }

    /**
     * 检查文本中是否包含敏感数据
     */
    public boolean containsSensitiveData(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }

        for (CompiledRule rule : compiledRules.values()) {
            if (rule.pattern.matcher(text).find()) {
                return true;
            }
        }
        return false;
    }

    /**
     * 检测文本中包含的敏感数据类型
     */
    public List<String> detectTypes(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }

        return compiledRules.entrySet().stream()
            .filter(entry -> entry.getValue().pattern.matcher(text).find())
            .map(Map.Entry::getKey)
            .toList();
    }

    /**
     * 编译后的规则
     */
    private record CompiledRule(String ruleCode, Pattern pattern, String maskFormat) {}
}
