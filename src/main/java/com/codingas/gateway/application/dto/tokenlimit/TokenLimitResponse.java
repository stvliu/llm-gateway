package com.codingas.gateway.adapter.admin.dto.tokenlimit;

import com.codingas.gateway.common.enums.ExceededAction;
import com.codingas.gateway.common.enums.PeriodType;
import com.codingas.gateway.domain.security.entity.TokenLimit.LimitType;
import com.codingas.gateway.domain.security.entity.TokenLimit.TokenLimitStatus;
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
    private TokenLimitStatus status;
    private Boolean enabled;
    private Instant createdAt;
    private Instant updatedAt;
}
