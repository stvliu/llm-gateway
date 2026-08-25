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

import java.math.BigDecimal;

/**
 * 更新 Token 限额用例入参
 *
 * <p>仅非 null 字段参与更新；enabled 映射状态 ACTIVE/SUSPENDED。</p>
 *
 * @param maxTokens        最大令牌数（可选）
 * @param periodType       周期类型（可选）
 * @param periodDayOfWeek  周周期日（可选）
 * @param periodDayOfMonth 月周期日（可选）
 * @param exceededAction   超限动作（可选）
 * @param switchModelId    切换模型 ID（可选）
 * @param enabled          是否启用（可选）
 */
public record TokenLimitUpdateCommand(
        BigDecimal maxTokens,
        PeriodType periodType,
        Integer periodDayOfWeek,
        Integer periodDayOfMonth,
        ExceededAction exceededAction,
        Long switchModelId,
        Boolean enabled
) {
}
