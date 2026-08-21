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
package com.codingas.gateway.usage.tokenlimit;
import com.codingas.gateway.common.entity.DomainEntity;
import com.codingas.gateway.common.entity.BaseEntity;

import com.codingas.gateway.usage.enums.ExceededAction;
import com.codingas.gateway.usage.enums.PeriodType;
import com.codingas.gateway.provider.model.Model;
import com.codingas.gateway.provider.vendor.Provider;
import com.codingas.gateway.iam.user.User;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Token 限额实体
 *
 * <p>用户级别 Token 使用限额，支持周期重置。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@DomainEntity
@Slf4j
public class TokenLimit extends BaseEntity {

    private User user;

    private Provider provider;

    private Model model;

    private LimitType limitType = LimitType.USER_CUSTOM;

    private BigDecimal maxTokens;

    private BigDecimal usedTokens = BigDecimal.ZERO;

    private PeriodType periodType = PeriodType.MONTHLY;

    private Integer periodDayOfWeek;

    private Integer periodDayOfMonth;

    private ExceededAction exceededAction = ExceededAction.REJECT;

    private Model switchModel;

    private TokenLimitState state = TokenLimitState.ACTIVE;

    private Instant deletedAt;

    public enum LimitType {
        /** 系统默认 */
        SYSTEM_DEFAULT,
        /** 用户自定义 */
        USER_CUSTOM
    }

    public enum TokenLimitState {
        /** 正常 */
        ACTIVE,
        /** 暂停 */
        SUSPENDED
    }

    /**
     * 检查是否超限
     */
    public boolean isExceeded() {
        return usedTokens.compareTo(maxTokens) >= 0;
    }
}
