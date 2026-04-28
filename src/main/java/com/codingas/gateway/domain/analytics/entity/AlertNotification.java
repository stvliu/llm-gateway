package com.codingas.gateway.domain.analytics.entity;

import com.codingas.gateway.common.entity.BaseEntity;
import com.codingas.gateway.domain.security.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;

/**
 * 预警通知实体
 *
 * <p>记录预警触发后的通知发送情况。</p>
 */
@Entity
@Table(name = "alert_notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AlertNotification extends BaseEntity {

    @Column(name = "notification_code", nullable = false, unique = true, length = 64)
    private String notificationCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alert_rule_id", nullable = false)
    private AlertRule alertRule;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_user_id", nullable = false)
    private User targetUser;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false)
    private AlertRule.NotificationChannel channel;

    @Column(name = "title", length = 256)
    private String title;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "alert_data", columnDefinition = "json")
    private Map<String, Object> alertData;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private NotificationStatus status = NotificationStatus.PENDING;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    public enum NotificationStatus {
        /** 待发送 */
        PENDING,
        /** 已发送 */
        SENT,
        /** 发送失败 */
        FAILED
    }
}