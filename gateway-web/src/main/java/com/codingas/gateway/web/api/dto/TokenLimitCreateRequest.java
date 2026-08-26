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
import com.codingas.gateway.usage.tokenlimit.TokenLimit.LimitType;
import com.codingas.gateway.usage.tokenlimit.TokenLimit;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 创建 Token 限额请求 DTO（HTTP 契约）
 */
@Data
public class TokenLimitCreateRequest {

    @NotNull(message = "User ID is required")
    private Long userId;

    private Long providerId;

    private Long modelId;

    private LimitType limitType = LimitType.USER_CUSTOM;

    @NotNull(message = "Max tokens is required")
    private BigDecimal maxTokens;

    private PeriodType periodType = PeriodType.MONTHLY;

    private Integer periodDayOfWeek;

    private Integer periodDayOfMonth;

    private ExceededAction exceededAction = ExceededAction.REJECT;

    private Long switchModelId;

    /**
     * 转换为限额实体（保持 DTO 默认值语义；userId 等 ID 经 create 单独传入）
     *
     * @return 限额实体
     */
    public TokenLimit toEntity() {
        TokenLimit tokenLimit = new TokenLimit();
        tokenLimit.setLimitType(limitType != null ? limitType : LimitType.USER_CUSTOM);
        tokenLimit.setMaxTokens(maxTokens);
        tokenLimit.setPeriodType(periodType != null ? periodType : PeriodType.MONTHLY);
        tokenLimit.setPeriodDayOfWeek(periodDayOfWeek);
        tokenLimit.setPeriodDayOfMonth(periodDayOfMonth);
        tokenLimit.setExceededAction(exceededAction != null ? exceededAction : ExceededAction.REJECT);
        return tokenLimit;
    }
}
