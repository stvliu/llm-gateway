/*
 * Copyright (c) 2025 codingas.com
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE file for details.
 */
package com.codingas.gateway.application.quota.dto;

import com.codingas.gateway.domain.usage.enums.ExceededAction;
import com.codingas.gateway.domain.usage.enums.PeriodType;
import com.codingas.gateway.domain.usage.entity.TokenLimit.LimitType;
import com.codingas.gateway.domain.usage.entity.TokenLimit.TokenLimitState;
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
