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
 * 创建用户用例入参
 *
 * <p>核心用户应用服务的创建入参，承载用户可编辑字段。
 * 字段合法性校验由 HTTP 层 DTO 承担（{@code web.api.dto.UserCreateRequest}）。</p>
 */
@Getter
@AllArgsConstructor
public class UserCreateCommand {

    /** 用户名 */
    private final String username;

    /** 邮箱 */
    private final String email;

    /** 密码（明文，由核心服务编码哈希后存储） */
    private final String password;

    /** 手机号 */
    private final String phone;

    /** 用户角色：ADMIN（管理员）/ USER（普通用户），为空时默认 USER */
    private final String role;
}
