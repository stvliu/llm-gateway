package com.codingas.gateway.domain.dataprotection.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * 敏感数据脱敏服务
 *
 * <p>支持手机号、身份证、银行卡等敏感数据的自动检测和脱敏。</p>
 * <p>使用内置默认规则，支持通过 reloadRules() 动态加载新规则。</p>
 */
@Slf4j
@Service
public class SensitiveDataMasker {

    // 默认脱敏规则
    private static final Map<String, CompiledRule> DEFAULT_RULES = Map.of(
        "PHONE", new CompiledRule("PHONE", Pattern.compile("(\\d{3})\\d{4}(\\d{4})"), "$1****$2"),
        "ID_CARD", new CompiledRule("ID_CARD", Pattern.compile("(\\d{6})\\d{8}(\\d{4})"), "$1********$2"),
        "BANK_CARD", new CompiledRule("BANK_CARD", Pattern.compile("(\\d{4})\\d+(\\d{4})"), "$1****$2")
    );

    // 运行时规则（从数据库加载）
    private Map<String, CompiledRule> compiledRules = new ConcurrentHashMap<>(DEFAULT_RULES);

    /**
     * 加载脱敏规则（从数据库）
     *
     * <p>TODO: 后续实现从数据库加载规则</p>
     */
    public void loadRules(List<RuleData> rules) {
        compiledRules.clear();
        compiledRules.putAll(DEFAULT_RULES);

        if (rules == null || rules.isEmpty()) {
            log.info("Using default sensitive data rules, count={}", compiledRules.size());
            return;
        }

        for (RuleData rule : rules) {
            try {
                CompiledRule compiled = new CompiledRule(
                    rule.code(),
                    Pattern.compile(rule.pattern()),
                    rule.maskFormat()
                );
                compiledRules.put(rule.code(), compiled);
                log.info("Loaded sensitive data rule: {} -> {}", rule.code(), rule.maskFormat());
            } catch (Exception e) {
                log.warn("Failed to compile rule {}: {}", rule.code(), e.getMessage());
            }
        }

        log.info("Loaded {} sensitive data rules", compiledRules.size());
    }

    /**
     * 重置为默认规则
     */
    public void resetToDefaultRules() {
        compiledRules.clear();
        compiledRules.putAll(DEFAULT_RULES);
        log.info("Reset to default sensitive data rules");
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
    private record CompiledRule(String code, Pattern pattern, String maskFormat) {}

    /**
     * 规则数据（用于从数据库加载）
     */
    public record RuleData(String code, String pattern, String maskFormat) {}
}
