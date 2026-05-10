package com.codingas.gateway.infrastructure.alert.gateway.database.dataobject;

import com.codingas.gateway.infrastructure.common.BaseDo;
import com.codingas.gateway.domain.usage.enums.PeriodType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * 预警规则 DO
 *
 * <p>JPA 实体，对应数据库 alert_rules 表。</p>
 */
@Entity
@Table(name = "alert_rules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AlertRuleDo extends BaseDo {

    @Column(name = "name", nullable = false, length = 128)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "alert_type", nullable = false)
    private AlertType alertType;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false)
    private TargetType targetType;

    @Column(name = "target_id")
    private Long targetId;

    @Enumerated(EnumType.STRING)
    @Column(name = "condition_type")
    private ConditionType conditionType;

    @Column(name = "threshold_value", precision = 20, scale = 6)
    private BigDecimal thresholdValue;

    @Enumerated(EnumType.STRING)
    @Column(name = "period_type")
    private PeriodType periodType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "notification_channels", columnDefinition = "json")
    private List<NotificationChannel> notificationChannels;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    public enum AlertType {
        USAGE,
        HEALTH,
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
}
