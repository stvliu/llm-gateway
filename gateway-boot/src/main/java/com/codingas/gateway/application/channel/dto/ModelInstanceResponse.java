/*
 * Copyright (c) 2025 codingas.com
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE file for details.
 */
package com.codingas.gateway.application.channel.dto;

import lombok.Data;

/**
 * 模型实例响应
 */
@Data
public class ModelInstanceResponse {

    private Long id;

    private Long channelId;

    private Long modelId;

    /** 供应商侧模型名称 */
    private String modelName;

    /** 模型展示名称 */
    private String displayName;

    /** 模型系列 */
    private String modelFamily;

    /** 上游模型名（为 null 表示与 modelName 相同） */
    private String upstreamModelName;

    /** 优先级 */
    private Integer priority;

    /** 权重 */
    private Integer weight;

    /** 关联状态 */
    private String state;
}