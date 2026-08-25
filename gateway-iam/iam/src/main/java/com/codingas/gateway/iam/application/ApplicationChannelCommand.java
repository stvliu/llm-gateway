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

/**
 * 应用-渠道授权项用例入参
 *
 * <p>表示一个应用授权的渠道及其应用级转移优先级。</p>
 *
 * @param channelId 渠道 ID（物理主键，必然 > 0）
 * @param priority  转移优先级（数值越小越优先；为 null 表示未配置，回退默认值 100）
 */
public record ApplicationChannelCommand(
        Long channelId,
        Integer priority
) {
}
