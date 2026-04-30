package com.codingas.gateway.domain.operations.entity;
import com.codingas.gateway.domain.DomainEntity;
import com.codingas.gateway.domain.BaseEntity;

import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.Map;

/**
 * 预警通知实体
 *
 * <p>记录预警触发后的通知发送情况。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@DomainEntity
@Slf4j
public class AlertNotification extends BaseEntity {

    private String notificationCode;

    private Long alertRuleId;

    private Long targetUserId;

    private NotificationChannel channel;

    private String title;

    private String content;

    private Map<String, Object> alertData;

    private NotificationStatus status = NotificationStatus.PENDING;

    private Instant sentAt;

    private Instant deletedAt;

    public enum NotificationStatus {
        /** 待发送 */
        PENDING,
        /** 已发送 */
        SENT,
        /** 发送失败 */
        FAILED
    }

    public enum NotificationChannel {
        SYSTEM,
        EMAIL,
        IM,
        SMS
    }
}
