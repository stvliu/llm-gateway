/*
 * Copyright (c) 2025 codingas.com
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE file for details.
 */
package com.codingas.gateway.infrastructure.dataprotection;

import com.codingas.gateway.domain.dataprotection.entity.SensitiveDataRule;
import com.codingas.gateway.domain.dataprotection.gateway.SensitiveDataRuleGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 敏感数据规则初始化器
 *
 * <p>在应用启动时插入默认的脱敏规则。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SensitiveDataRuleInitializer implements CommandLineRunner {

    private final SensitiveDataRuleGateway ruleGateway;

    @Override
    public void run(String... args) {
        if (ruleGateway.count() > 0) {
            log.info("Sensitive data rules already initialized, skipping");
            return;
        }

        List<SensitiveDataRule> defaultRules = List.of(
            createRule("phone", "PHONE",
                "\\d{11}", "138****5678"),
            createRule("id_card", "ID_CARD",
                "[1-9]\\d{5}(19|20)\\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\\d|3[01])\\d{3}[\\dXx]", "320***********1234"),
            createRule("bank_card", "BANK_CARD",
                "\\d{16,19}", "**** **** **** 1234"),
            createRule("email", "EMAIL",
                "[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}", "a***@example.com"),
            createRule("address", "ADDRESS",
                "\\d{5,6}", "*****"),
            createRule("password", "PASSWORD",
                "(?i)(password|pwd|pass|secret|token|key)\\s*[:=]\\s*[^\\s,}]+", "***"),
            createRule("credit_card", "CREDIT_CARD",
                "\\d{4}[- ]?\\d{4}[- ]?\\d{4}[- ]?\\d{4}", "**** **** **** ****"),
            createRule("ssn", "SSN",
                "\\d{3}-\\d{2}-\\d{4}", "***-**-****")
        );

        ruleGateway.saveAll(defaultRules);
        log.info("Initialized {} sensitive data rules", defaultRules.size());
    }

    private SensitiveDataRule createRule(String code, String type, String pattern, String maskFormat) {
        SensitiveDataRule rule = new SensitiveDataRule();
        rule.setRuleName(code);
        rule.setDataType(type);
        rule.setRegexPattern(pattern);
        rule.setMaskFormat(maskFormat);
        rule.setEnabled(true);
        return rule;
    }
}
