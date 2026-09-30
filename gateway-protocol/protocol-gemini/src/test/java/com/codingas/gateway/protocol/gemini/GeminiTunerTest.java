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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * GeminiTuner 测试 — 协议级默认值补全
 */
class GeminiTunerTest {

    private final GeminiTuner tuner = new GeminiTuner();

    @Test
    void getProtocol_返回gemini() {
        assertThat(tuner.getProtocol()).isEqualTo("gemini");
    }

    @Test
    void tune_maxTokens缺省_补4096() {
        GeminiChatRequest req = new GeminiChatRequest();
        req.setModel("gemini-2.5-pro");

        GeminiChatRequest tuned = tuner.tune(req);

        assertThat(tuned.getMaxTokens()).isEqualTo(4096);
    }

    @Test
    void tune_maxTokens已设置_不覆盖() {
        GeminiChatRequest req = new GeminiChatRequest();
        req.setModel("gemini-2.5-pro");
        req.setMaxTokens(100);

        GeminiChatRequest tuned = tuner.tune(req);

        assertThat(tuned.getMaxTokens()).isEqualTo(100);
    }

    @Test
    void tune_保持其他字段不变() {
        GeminiChatRequest req = new GeminiChatRequest();
        req.setModel("gemini-2.5-pro");
        req.setSystem("s");
        req.addMessage(new GeminiChatRequest.Message("user", "hi"));
        req.setTemperature(0.7);

        GeminiChatRequest tuned = tuner.tune(req);

        assertThat(tuned.getModel()).isEqualTo("gemini-2.5-pro");
        assertThat(tuned.getSystem()).isEqualTo("s");
        assertThat(tuned.getMessages()).hasSize(1);
        assertThat(tuned.getTemperature()).isEqualTo(0.7);
        assertThat(tuned.getMaxTokens()).isEqualTo(4096);
    }
}
