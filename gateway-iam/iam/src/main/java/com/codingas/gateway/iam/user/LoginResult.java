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

import java.util.List;

/**
 * 用户登录用例结果
 *
 * <p>承载登录成功的用户实体、会话令牌与角色推导的权限码
 * （前端 UI 直接消费权限码，不自行维护映射）。</p>
 *
 * @param user        已认证用户实体
 * @param token       会话令牌
 * @param permissions 角色推导的权限码列表
 */
public record LoginResult(
        User user,
        String token,
        List<String> permissions
) {
}
