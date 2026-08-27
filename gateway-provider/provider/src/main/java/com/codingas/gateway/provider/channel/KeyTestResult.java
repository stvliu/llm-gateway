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
package com.codingas.gateway.provider.channel;

import java.util.Collections;
import java.util.List;

/**
 * 单 Key 测试结果（内部不可变对象）
 *
 * <p>由 {@link com.codingas.gateway.provider.channel.ChannelKeyProbe} 返回，
 * 由 com.codingas.gateway.provider.service.ChannelHealthService
 * 聚合并脱敏后形成对外 {@link KeyMatrixRow}。</p>
 *
 * @param credentialId    凭证 ID
 * @param apiKeyPlain     Key 明文（脱敏前），仅在内部传递
 * @param auth            认证状态
 * @param errorMessage    失败/超时时的错误说明，PASS 时可为 null
 * @param availableModels 可用模型列表，FAIL/TIMEOUT 时为空
 * @param latencyMs       延迟毫秒数；FAIL/TIMEOUT 时为 null
 */
public record KeyTestResult(
        Long credentialId,
        String apiKeyPlain,
        AuthStatus auth,
        String errorMessage,
        List<String> availableModels,
        Long latencyMs
) {

    /**
     * 创建一个 PASS 结果（必带可用模型列表与延迟）
     */
    public static KeyTestResult pass(Long credentialId, String apiKeyPlain,
                                     List<String> availableModels, Long latencyMs) {
        return new KeyTestResult(credentialId, apiKeyPlain, AuthStatus.PASS, null,
                availableModels == null ? Collections.emptyList() : List.copyOf(availableModels),
                latencyMs);
    }

    /**
     * 创建一个 FAIL 结果，可用模型列表为空
     */
    public static KeyTestResult fail(Long credentialId, String apiKeyPlain, String errorMessage) {
        return new KeyTestResult(credentialId, apiKeyPlain, AuthStatus.FAIL, errorMessage,
                Collections.emptyList(), null);
    }

    /**
     * 创建一个 TIMEOUT 结果，可用模型列表为空
     */
    public static KeyTestResult timeout(Long credentialId, String apiKeyPlain) {
        return new KeyTestResult(credentialId, apiKeyPlain, AuthStatus.TIMEOUT, "TIMEOUT",
                Collections.emptyList(), null);
    }
}
