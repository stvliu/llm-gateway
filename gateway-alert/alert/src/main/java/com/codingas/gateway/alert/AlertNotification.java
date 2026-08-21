/*
 * Copyright © 2025-2026 codingas.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.codingas.gateway.alert;
import com.codingas.gateway.common.entity.DomainEntity;
import com.codingas.gateway.common.entity.BaseEntity;

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
