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

import com.codingas.gateway.iam.user.ResetPasswordResult;

/**
 * 重置密码响应 DTO（HTTP 契约）
 *
 * @param newPassword 一次性明文临时密码
 */
public record ResetPasswordResponse(String newPassword) {
    /**
     * 从重置密码用例结果转换
     *
     * @param result 重置密码用例结果
     * @return 重置密码响应 DTO
     */
    public static ResetPasswordResponse from(ResetPasswordResult result) {
        return new ResetPasswordResponse(result.newPassword());
    }
}
