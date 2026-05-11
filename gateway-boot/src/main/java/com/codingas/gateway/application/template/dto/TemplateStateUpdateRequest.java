package com.codingas.gateway.application.template.dto;

import com.codingas.gateway.domain.template.entity.MarketState;
import jakarta.validation.constraints.NotNull;

/**
 * 模板状态更新请求
 */
public record TemplateStateUpdateRequest(
    @NotNull(message = "状态不能为空")
    MarketState marketState
) {}
