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
import com.codingas.gateway.usage.tokenlimit.TokenLimitCreateCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 创建 Token 限额请求 DTO（HTTP 契约）
 */
@Data
public class TokenLimitCreateRequest {

    @NotBlank(message = "Limit code is required")
    @Size(max = 64, message = "Limit code must not exceed 64 characters")
    private String limitCode;

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
     * 转换为核心创建用例入参（保持 DTO 默认值语义）
     *
     * @return 创建用例入参
     */
    public TokenLimitCreateCommand toCommand() {
        return new TokenLimitCreateCommand(
                limitCode, userId, providerId, modelId,
                limitType != null ? limitType : LimitType.USER_CUSTOM,
                maxTokens,
                periodType != null ? periodType : PeriodType.MONTHLY,
                periodDayOfWeek, periodDayOfMonth,
                exceededAction != null ? exceededAction : ExceededAction.REJECT,
                switchModelId);
    }
}
