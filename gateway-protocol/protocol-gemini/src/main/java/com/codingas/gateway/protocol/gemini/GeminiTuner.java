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

import com.codingas.gateway.protocol.tuning.ProtocolTuner;

/**
 * Gemini 协议出站调谐器
 *
 * <p>补全 Gemini 请求的默认值：</p>
 * <ul>
 *   <li>maxTokens（序列化为 generationConfig.maxOutputTokens）缺省补 4096</li>
 * </ul>
 *
 * <p>模型名替换由 {@code OutboundTuner} 渠道级调谐对任意协议统一执行，
 * 本调谐器只做协议级默认值补全（与 OpenAITuner/AnthropicTuner 对齐）。</p>
 */
public class GeminiTuner implements ProtocolTuner<GeminiChatRequest> {

    private static final int DEFAULT_MAX_TOKENS = 4096;

    @Override
    public String getProtocol() {
        return "gemini";
    }

    @Override
    public GeminiChatRequest tune(GeminiChatRequest request) {
        // 补全 maxTokens 默认值
        if (request.getMaxTokens() == null) {
            request.setMaxTokens(DEFAULT_MAX_TOKENS);
        }
        return request;
    }
}
