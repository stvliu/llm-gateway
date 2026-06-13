package com.codingas.gateway.adapter.api.dto;

import com.codingas.gateway.domain.supply.enums.ChannelHealthSource;
import jakarta.validation.constraints.NotNull;

/**
 * 渠道健康检查请求体
 *
 * @param source 触发来源（CARD / DRAWER / PRECHECK），必填
 */
public record ChannelHealthCheckRequest(
        @NotNull(message = "source 字段必填") ChannelHealthSource source
) {
}
