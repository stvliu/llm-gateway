package com.codingas.gateway.application.quota.dto;

import com.codingas.gateway.domain.usage.enums.ExceededAction;
import com.codingas.gateway.domain.usage.enums.PeriodType;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 更新 Token 限额请求
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
}
