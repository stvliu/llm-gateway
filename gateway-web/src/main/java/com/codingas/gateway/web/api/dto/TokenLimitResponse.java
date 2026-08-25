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

import com.codingas.gateway.common.dto.PageResponse;
import com.codingas.gateway.provider.model.Model;
import com.codingas.gateway.usage.enums.ExceededAction;
import com.codingas.gateway.usage.enums.PeriodType;
import com.codingas.gateway.usage.tokenlimit.TokenLimit;
import com.codingas.gateway.usage.tokenlimit.TokenLimit.LimitType;
import com.codingas.gateway.usage.tokenlimit.TokenLimit.TokenLimitState;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Token 限额响应 DTO（HTTP 契约）
 *
 * <p>由 {@link #from(TokenLimit)} 从 {@code TokenLimit} 实体展开关联对象后生成。</p>
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

    /**
     * 从限额实体转换（展开关联对象的展示字段）
     *
     * @param tokenLimit 限额实体
     * @return 限额响应 DTO
     */
    public static TokenLimitResponse from(TokenLimit tokenLimit) {
        TokenLimitResponse response = new TokenLimitResponse();
        response.setId(tokenLimit.getId());
        response.setUserId(tokenLimit.getUser().getId());
        response.setUsername(tokenLimit.getUser().getUsername());
        if (tokenLimit.getProvider() != null) {
            response.setProviderId(tokenLimit.getProvider().getId());
            response.setProviderName(tokenLimit.getProvider().getName());
        }
        if (tokenLimit.getModel() != null) {
            response.setModelId(tokenLimit.getModel().getId());
            response.setModelName(tokenLimit.getModel() instanceof Model m ? m.getDisplayName() : null);
        }
        response.setLimitType(tokenLimit.getLimitType());
        response.setMaxTokens(tokenLimit.getMaxTokens());
        response.setUsedTokens(tokenLimit.getUsedTokens());
        if (tokenLimit.getMaxTokens() != null && tokenLimit.getUsedTokens() != null) {
            response.setRemainingTokens(tokenLimit.getMaxTokens().subtract(tokenLimit.getUsedTokens()));
        }
        response.setPeriodType(tokenLimit.getPeriodType());
        response.setPeriodDayOfWeek(tokenLimit.getPeriodDayOfWeek());
        response.setPeriodDayOfMonth(tokenLimit.getPeriodDayOfMonth());
        response.setExceededAction(tokenLimit.getExceededAction());
        if (tokenLimit.getSwitchModel() != null) {
            response.setSwitchModelId(tokenLimit.getSwitchModel().getId());
            response.setSwitchModelName(tokenLimit.getSwitchModel() instanceof Model m ? m.getDisplayName() : null);
        }
        response.setState(tokenLimit.getState());
        response.setEnabled(tokenLimit.getState() == TokenLimitState.ACTIVE);
        response.setCreatedAt(tokenLimit.getCreatedAt());
        response.setUpdatedAt(tokenLimit.getUpdatedAt());
        return response;
    }

    /**
     * 从限额实体分页转换
     *
     * @param page 限额实体分页
     * @return 限额响应 DTO 分页
     */
    public static PageResponse<TokenLimitResponse> fromPage(PageResponse<TokenLimit> page) {
        return PageResponse.of(
                page.getItems().stream().map(TokenLimitResponse::from).toList(),
                page.getPagination().getPage(),
                page.getPagination().getLimit(),
                page.getPagination().getTotal());
    }
}
