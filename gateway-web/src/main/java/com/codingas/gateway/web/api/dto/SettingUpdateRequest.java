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

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 系统设置更新请求 DTO
 *
 * <p>PUT /api/v1/settings/{key} 的请求体：携带目标设置项的新值。</p>
 */
@Data
public class SettingUpdateRequest {

    /** 设置新值（不允许为空） */
    @NotNull(message = "value 不能为空")
    private String value;
}
