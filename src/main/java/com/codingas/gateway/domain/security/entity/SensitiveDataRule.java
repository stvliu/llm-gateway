package com.codingas.gateway.domain.security.entity;

import com.codingas.gateway.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * 敏感数据规则实体
 *
 * <p>定义敏感数据的检测和脱敏规则。</p>
 * <p>通过正则表达式匹配敏感数据，并使用 maskFormat 指定的格式进行脱敏。</p>
 * <p>maskFormat 示例: ****1234（保留后4位）、****（全部隐藏）</p>
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