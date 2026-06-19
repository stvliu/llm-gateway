package com.codingas.gateway.adapter.api;

import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * 应用渠道授权更新请求 DTO
 *
 * <p>承载应用授权的渠道 ID 列表，用于 PUT /api/v1/applications/{id}/channels。</p>
 *
 * @param channelIds 渠道 ID 列表（空列表表示清空全部授权）
 */
public record ApplicationChannelRequest(
        @NotNull(message = "channelIds 不能为 null，空列表请传 []")
        List<Long> channelIds
) {
}
