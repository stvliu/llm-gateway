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
package com.codingas.gateway.usage.dto;

import com.codingas.gateway.usage.enums.ExceededAction;
import com.codingas.gateway.usage.enums.PeriodType;
import com.codingas.gateway.usage.tokenlimit.TokenLimit.LimitType;
import com.codingas.gateway.usage.tokenlimit.TokenLimit.TokenLimitState;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Token 限额响应
 */
@Data
public class TokenLimitResponse {

    private Long id;
    private String limitCode;
    private Long userId;
    private String username;
    private Long providerId;
    private String providerName;
    private Long modelId;
    private String modelName;
    private LimitType limitType;
    private BigDecimal maxTokens;
    private BigDecimal usedTokens;
    private BigDecimal remainingTokens;
    private PeriodType periodType;
    private Integer periodDayOfWeek;
    private Integer periodDayOfMonth;
    private ExceededAction exceededAction;
    private Long switchModelId;
    private String switchModelName;
    private TokenLimitState state;
    private Boolean enabled;
    private Instant createdAt;
    private Instant updatedAt;
}
