/*
 * Copyright (c) 2025 codingas.com
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE file for details.
 */
package com.codingas.gateway.application.channel.dto;

import lombok.Data;

/**
 * 模型实例更新请求
 *
 * <p>支持更新模型 ID 和上游模型名。channelId 由适配层（Controller）从协议上下文中提取并填充。
 * 字段为 null 表示不更新该字段。</p>
 */
@Data
public class ModelInstanceUpdateRequest {

    /** 渠道 ID（适配层填充） */
    private Long channelId;

    /** 新模型 ID，为 null 表示不更新 */
    private Long modelId;

    /** 上游模型名，为 null 表示不更新 */
    private String upstreamModelName;
}
