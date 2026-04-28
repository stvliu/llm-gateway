package com.codingas.gateway.adapter.admin.dto.tokenlimit;

import com.codingas.gateway.common.enums.ExceededAction;
import com.codingas.gateway.common.enums.PeriodType;
import com.codingas.gateway.domain.security.entity.TokenLimit.LimitType;
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
