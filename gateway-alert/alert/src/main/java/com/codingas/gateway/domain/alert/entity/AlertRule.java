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
package com.codingas.gateway.domain.alert.entity;
import com.codingas.gateway.common.entity.DomainEntity;
import com.codingas.gateway.common.entity.BaseEntity;

import com.codingas.gateway.usage.enums.PeriodType;
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
