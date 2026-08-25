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

import com.codingas.gateway.usage.enums.ExceededAction;
import com.codingas.gateway.usage.enums.PeriodType;
import com.codingas.gateway.usage.tokenlimit.TokenLimit.LimitType;

import java.math.BigDecimal;

/**
 * 创建 Token 限额用例入参
 *
 * <p>默认值语义由 web 层 DTO 的 toCommand() 保证（limitType=USER_CUSTOM、
 * periodType=MONTHLY、exceededAction=REJECT）。</p>
 *
 * @param limitCode        限额编码（全局唯一）
 * @param userId           用户 ID（必填）
 * @param providerId       提供商 ID（可选）
 * @param modelId          模型 ID（可选）
 * @param limitType        限额类型（默认 USER_CUSTOM）
 * @param maxTokens        最大令牌数（必填）
 * @param periodType       周期类型（默认 MONTHLY）
 * @param periodDayOfWeek  周周期日（可选）
 * @param periodDayOfMonth 月周期日（可选）
 * @param exceededAction   超限动作（默认 REJECT）
 * @param switchModelId    切换模型 ID（可选）
 */
public record TokenLimitCreateCommand(
        String limitCode,
        Long userId,
        Long providerId,
        Long modelId,
        LimitType limitType,
        BigDecimal maxTokens,
        PeriodType periodType,
        Integer periodDayOfWeek,
        Integer periodDayOfMonth,
        ExceededAction exceededAction,
        Long switchModelId
) {
}
