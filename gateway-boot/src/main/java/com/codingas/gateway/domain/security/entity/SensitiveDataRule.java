package com.codingas.gateway.domain.security.entity;

import com.codingas.gateway.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * 敏感数据规则实体
 */
@Entity
@Table(name = "sensitive_data_rules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SensitiveDataRule extends BaseEntity {

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