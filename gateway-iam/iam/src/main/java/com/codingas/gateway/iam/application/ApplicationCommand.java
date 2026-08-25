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
package com.codingas.gateway.iam.application;

import com.codingas.gateway.common.enums.FailureStrategy;

/**
 * 应用创建/更新用例入参
 *
 * <p>承载应用聚合根可编辑字段：code（全局唯一）、name、description、timeout。
 * state 由后端管理（创建时默认 ACTIVE），不通过本对象修改。</p>
 *
 * @param code             应用编码（全局唯一）
 * @param name             应用名称
 * @param description      应用描述
 * @param timeout          请求超时秒数（0 表示用渠道默认）
 * @param failureStrategy  应用级失败处理策略（为空时默认 FAIL_RETRY）
 */
public record ApplicationCommand(
        String code,
        String name,
        String description,
        int timeout,
        FailureStrategy failureStrategy
) {
}
