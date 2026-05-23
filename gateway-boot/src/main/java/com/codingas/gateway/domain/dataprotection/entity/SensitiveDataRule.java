package com.codingas.gateway.domain.dataprotection.entity;
import com.codingas.gateway.common.entity.DomainEntity;
import com.codingas.gateway.common.entity.BaseEntity;

import lombok.*;
import lombok.extern.slf4j.Slf4j;

/**
 * 敏感数据规则实体
 *
 * <p>定义敏感数据的检测和脱敏规则。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@DomainEntity
@Slf4j
public class SensitiveDataRule extends BaseEntity {

    private String ruleName;

    private String dataType;

    private String regexPattern;

    private String maskFormat;

    private Boolean enabled;

    /**
     * 检查规则是否启用
     */
    public boolean isEnabled() {
        return Boolean.TRUE.equals(enabled);
    }
}
