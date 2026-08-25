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

import com.codingas.gateway.iam.application.ApplicationChannel;
import com.codingas.gateway.iam.application.ApplicationChannelCommand;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

/**
 * 应用-渠道授权项 DTO（HTTP 契约）
 *
 * <p>表示一个应用授权的渠道及其应用级转移优先级。请求与响应共用此结构。</p>
 *
 * @param channelId 渠道 ID（物理主键 BIGINT AUTO_INCREMENT，必然 > 0）
 * @param priority  转移优先级（数值越小越优先；为 null 表示未配置，回退默认值 100）
 */
public record ApplicationChannelItem(
        @NotNull(message = "channelId 不能为 null")
        @Positive(message = "channelId 必须为正数")
        Long channelId,
        Integer priority
) {
    /**
     * 从渠道授权关联实体转换
     *
     * @param rel 渠道授权关联实体
     * @return 渠道授权项 DTO
     */
    public static ApplicationChannelItem from(ApplicationChannel rel) {
        return new ApplicationChannelItem(rel.getChannelId(), rel.getPriority());
    }

    /**
     * 从渠道授权关联实体列表转换
     *
     * @param rels 渠道授权关联实体列表
     * @return 渠道授权项 DTO 列表
     */
    public static List<ApplicationChannelItem> from(List<ApplicationChannel> rels) {
        return rels.stream().map(ApplicationChannelItem::from).toList();
    }

    /**
     * 转换为核心渠道授权用例入参
     *
     * @return 渠道授权用例入参
     */
    public ApplicationChannelCommand toCommand() {
        return new ApplicationChannelCommand(channelId, priority);
    }
}
