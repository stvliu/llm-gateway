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
package com.codingas.gateway.iam.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import java.util.List;

/**
 * 用户角色分配请求
 *
 * <p>简化角色模型：仅支持分配单一角色（取列表第一个）。</p>
 */
@Data
public class UserRoleAssignRequest {
    /**
     * 角色代码列表（简化模型下仅使用第一个）
     */
    @NotEmpty(message = "角色代码不能为空")
    private List<String> roleCodes;
}
