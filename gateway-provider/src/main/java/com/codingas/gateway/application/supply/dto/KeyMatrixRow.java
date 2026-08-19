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
package com.codingas.gateway.application.supply.dto;

import java.util.List;

/**
 * 健康测试矩阵行（脱敏后对外暴露）
 *
 * @param credentialId    凭证 ID
 * @param keyMasked       脱敏后的 Key（如 sk-***...wxyz）
 * @param auth            认证状态
 * @param authError       失败/超时时的错误说明
 * @param availableModels 可用模型列表
 * @param latencyMs       延迟毫秒数；FAIL/TIMEOUT 时为 null
 */
public record KeyMatrixRow(
        Long credentialId,
        String keyMasked,
        AuthStatus auth,
        String authError,
        List<String> availableModels,
        Long latencyMs
) {
}
