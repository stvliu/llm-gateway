package com.codingas.gateway.core.domain.entity;

import com.codingas.gateway.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * 敏感数据规则实体
 *
 * <p>定义数据脱敏规则，支持对请求/响应中的敏感数据进行检测和脱敏。</p>
 *
 * <p>表名: sensitive_data_rules</p>
 *
 * @see BaseEntity
 */
@Entity
@Table(
    name = "sensitive_data_rules",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_sensitive_data_rules_rule_code", columnNames = "rule_code")
    }
)
@Getter
@Setter
public class SensitiveDataRule extends BaseEntity {

    /**
     * 规则编码 (业务标识, 如 "phone", "id_card", "bank_card")
     */
    @Column(name = "rule_code", nullable = false, unique = true, length = 64)
    private String ruleCode;

    /**
     * 数据类型
     *
     * <p>如 "PHONE", "ID_CARD", "BANK_CARD", "EMAIL"</p>
     */
    @Column(name = "data_type", nullable = false, length = 32)
    private String dataType;

    /**
     * 正则表达式模式 (用于检测)
     */
    @Column(name = "pattern", nullable = false, length = 256)
    private String pattern;

    /**
     * 脱敏格式
     *
     * <p>如 "138****5678" 表示保留前3位和后4位</p>
     */
    @Column(name = "mask_format", nullable = false, length = 64)
    private String maskFormat;

    /**
     * 规则描述
     */
    @Column(name = "description", length = 128)
    private String description;

    /**
     * 规则是否启用
     */
    @Column(name = "enabled", nullable = false)
    private Boolean enabled = true;

    /**
     * 优先级 (数值越大优先级越高)
     */
    @Column(name = "priority", nullable = false)
    private Integer priority = 0;
}
