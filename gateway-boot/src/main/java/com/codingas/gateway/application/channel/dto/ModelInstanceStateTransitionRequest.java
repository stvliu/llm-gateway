/*
 * Copyright (c) 2025 codingas.com
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE file for details.
 */
package com.codingas.gateway.application.channel.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 模型实例状态转换请求
 */
@Data
public class ModelInstanceStateTransitionRequest {

    /** 目标状态（PENDING / ACTIVE / SUSPENDED / DEPRECATED / RETIRED） */
    @NotBlank(message = "目标状态不能为空")
    private String targetState;
}
