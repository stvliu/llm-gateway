package com.codingas.gateway.application.quota.dto;

import com.codingas.gateway.common.enums.ExceededAction;
import com.codingas.gateway.common.enums.PeriodType;
import com.codingas.gateway.domain.usage.entity.TokenLimit.LimitType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 创建 Token 限额请求
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
}
