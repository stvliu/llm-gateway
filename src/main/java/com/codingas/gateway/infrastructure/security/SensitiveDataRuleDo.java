package com.codingas.gateway.infrastructure.security;

import com.codingas.gateway.infrastructure.common.BaseDo;
import jakarta.persistence.*;
import lombok.*;

/**
 * 敏感数据规则 DO
 *
 * <p>JPA 实体，对应数据库 sensitive_data_rules 表。</p>
 */
@Entity
@Table(name = "sensitive_data_rules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SensitiveDataRuleDo extends BaseDo {

    @Column(name = "rule_name", nullable = false)
    private String ruleName;

    @Column(name = "data_type", nullable = false)
    private String dataType;

    @Column(name = "regex_pattern", nullable = false)
    private String regexPattern;

    @Column(name = "mask_format")
    private String maskFormat;

    @Column(name = "enabled")
    private Boolean enabled;
}
