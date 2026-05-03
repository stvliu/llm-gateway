package com.codingas.gateway.infrastructure.alert.gateway.database.dataobject;

import com.codingas.gateway.infrastructure.common.BaseDo;
import com.codingas.gateway.infrastructure.user.gateway.database.dataobject.UserDo;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;

/**
 * 预警通知 DO
 *
 * <p>JPA 实体，对应数据库 alert_notifications 表。</p>
 */
@Entity
@Table(name = "alert_notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AlertNotificationDo extends BaseDo {

    @Column(name = "notification_code", nullable = false, unique = true, length = 64)
    private String notificationCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alert_rule_id", nullable = false)
    private AlertRuleDo alertRule;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_user_id", nullable = false)
    private UserDo targetUser;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false)
    private AlertNotificationDo.NotificationChannel channel;

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
        PENDING,
        SENT,
        FAILED
    }

    public enum NotificationChannel {
        SYSTEM,
        EMAIL,
        IM,
        SMS
    }
}
