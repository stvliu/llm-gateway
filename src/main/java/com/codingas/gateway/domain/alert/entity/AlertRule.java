package com.codingas.gateway.domain.alert.entity;
import com.codingas.gateway.domain.DomainEntity;
import com.codingas.gateway.domain.BaseEntity;

import com.codingas.gateway.common.enums.PeriodType;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * 预警规则实体
 *
 * <p>定义预警触发条件，包括用量预警、健康预警、额度预警。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@DomainEntity
@Slf4j
public class AlertRule extends BaseEntity {

    private String ruleCode;

    private String name;

    private AlertType alertType;

    private TargetType targetType;

    private Long targetId;

    private ConditionType conditionType;

    private BigDecimal thresholdValue;

    private PeriodType periodType;

    private List<NotificationChannel> notificationChannels;

    private Boolean isActive = true;

    private Instant deletedAt;

    public enum AlertType {
        /** 用量预警 */
        USAGE,
        /** 健康预警 */
        HEALTH,
        /** 额度预警 */
        QUOTA
    }

    public enum TargetType {
        USER,
        PROVIDER,
        API_KEY
    }

    public enum ConditionType {
        THRESHOLD,
        RATIO,
        TREND
    }

    public enum NotificationChannel {
        SYSTEM,
        EMAIL,
        IM,
        SMS
    }

    /**
     * 检查规则是否激活
     */
    public boolean isActive() {
        return Boolean.TRUE.equals(isActive);
    }
}
