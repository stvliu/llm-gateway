package com.codingas.gateway.adapter.api;

import com.codingas.gateway.application.application.dto.ApplicationChannelItem;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * 应用渠道授权更新请求 DTO
 *
 * <p>承载应用授权的渠道及其应用级转移优先级列表，用于 PUT /api/v1/applications/{id}/channels。</p>
 *
 * <p>Task gap2：转移顺序改为应用级 priority，本请求由纯 channelIds 升级为
 * 含 priority 的列表。空列表表示清空全部授权；每个元素的 channelId 必须非 null 且为正数。</p>
 *
 * @param channels 渠道授权项列表（channelId + priority）
 */
public record ApplicationChannelRequest(
        @NotNull(message = "channels 不能为 null，空列表请传 []")
        List<@Valid ApplicationChannelItem> channels
) {
}
