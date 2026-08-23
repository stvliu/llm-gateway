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
package com.codingas.gateway.protocol.openai;

import com.codingas.gateway.protocol.contract.OpenAIChatRequest;
import com.codingas.gateway.protocol.tuning.ProtocolTuner;

/**
 * OpenAI 协议出站调谐器
 *
 * <p>补全 OpenAI 请求的默认值：</p>
 * <ul>
 *   <li>max_tokens 缺省补 4096</li>
 * </ul>
 */
public class OpenAITuner implements ProtocolTuner<OpenAIChatRequest> {

    private static final int DEFAULT_MAX_TOKENS = 4096;

    @Override
    public String getProtocol() {
        return "openai";
    }

    @Override
    public OpenAIChatRequest tune(OpenAIChatRequest request) {
        // 补全 max_tokens 默认值
        if (request.getMaxTokens() == null) {
            request.setMaxTokens(DEFAULT_MAX_TOKENS);
        }
        return request;
    }
}