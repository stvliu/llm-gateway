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
package com.codingas.gateway.protocol.gemini;

import com.codingas.gateway.protocol.ProtocolResponse;

/**
 * Gemini 原生响应契约（简化模型）。
 *
 * @param id            响应 ID（Gemini 响应无 id 字段，可为 null）
 * @param model         模型名
 * @param text          文本内容
 * @param inputTokens   输入 token 数
 * @param outputTokens  输出 token 数
 * @param finishReason  Gemini 结束原因（STOP/MAX_TOKENS/SAFETY 等，可为 null）
 */
public record GeminiChatResponse(String id, String model, String text,
                                 Integer inputTokens, Integer outputTokens, String finishReason)
        implements ProtocolResponse {

    @Override
    public String getModel() {
        return model;
    }

    @Override
    public String getFinishReason() {
        return finishReason;
    }
}
