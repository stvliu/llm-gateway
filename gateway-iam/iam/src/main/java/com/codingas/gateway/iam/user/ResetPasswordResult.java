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

/**
 * 重置密码用例结果
 *
 * <p>承载一次性明文的临时密码（仅重置时返回一次，后续不可获取）。</p>
 *
 * @param newPassword 一次性明文临时密码
 */
public record ResetPasswordResult(
        String newPassword
) {
}
