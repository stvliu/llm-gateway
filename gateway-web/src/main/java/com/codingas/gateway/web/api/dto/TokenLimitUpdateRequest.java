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
package com.codingas.gateway.web.api.dto;

import com.codingas.gateway.usage.enums.ExceededAction;
import com.codingas.gateway.usage.enums.PeriodType;
import com.codingas.gateway.usage.tokenlimit.TokenLimit;
import com.codingas.gateway.usage.tokenlimit.TokenLimit.TokenLimitState;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 更新 Token 限额请求 DTO（HTTP 契约）
 */
@Data
public class TokenLimitUpdateRequest {

    private BigDecimal maxTokens;

    private PeriodType periodType;

    private Integer periodDayOfWeek;

    private Integer periodDayOfMonth;

    private ExceededAction exceededAction;

    private Long switchModelId;

    private Boolean enabled;

    /**
     * 转换为限额实体（null 字段表示不更新；enabled 映射状态）
     *
     * @return 限额实体
     */
    public TokenLimit toEntity() {
        TokenLimit tokenLimit = new TokenLimit();
        tokenLimit.setMaxTokens(maxTokens);
        tokenLimit.setPeriodType(periodType);
        tokenLimit.setPeriodDayOfWeek(periodDayOfWeek);
        tokenLimit.setPeriodDayOfMonth(periodDayOfMonth);
        tokenLimit.setExceededAction(exceededAction);
        if (enabled != null) {
            tokenLimit.setState(enabled ? TokenLimitState.ACTIVE : TokenLimitState.SUSPENDED);
        }
        return tokenLimit;
    }
}
