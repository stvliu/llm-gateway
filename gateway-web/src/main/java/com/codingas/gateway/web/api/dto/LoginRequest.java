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

import com.codingas.gateway.iam.user.LoginCommand;
import jakarta.validation.constraints.NotBlank;

/**
 * 登录请求 DTO（HTTP 契约）
 */
public record LoginRequest(
    @NotBlank(message = "用户名不能为空")
    String username,

    @NotBlank(message = "密码不能为空")
    String password,

    boolean rememberMe
) {
    /**
     * 转换为核心登录用例入参
     *
     * @return 登录用例入参
     */
    public LoginCommand toCommand() {
        return new LoginCommand(username, password, rememberMe);
    }
}
