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
package com.codingas.gateway.iam.user;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 更新用户用例入参
 *
 * <p>核心用户应用服务的部分更新入参：仅非 null 字段参与更新。
 * 字段合法性校验由 HTTP 层 DTO 承担（{@code web.api.dto.UserUpdateRequest}）。</p>
 */
@Getter
@AllArgsConstructor
public class UserUpdateCommand {

    /** 用户名（可为 null，表示不更新） */
    private final String username;

    /** 邮箱（可为 null，表示不更新） */
    private final String email;

    /** 手机号（可为 null，表示不更新） */
    private final String phone;

    /** 头像 URL（可为 null，表示不更新） */
    private final String avatarUrl;
}
