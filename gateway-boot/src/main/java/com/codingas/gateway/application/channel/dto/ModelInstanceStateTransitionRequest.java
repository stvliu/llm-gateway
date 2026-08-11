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
