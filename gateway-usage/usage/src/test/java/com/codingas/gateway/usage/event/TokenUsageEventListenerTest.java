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
package com.codingas.gateway.usage.event;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * TokenUsageEventListener 单元测试
 *
 * <p>监听器仅记录日志，验证任意合法事件可被处理且不抛异常。</p>
 */
@DisplayName("TokenUsageEventListener 测试")
class TokenUsageEventListenerTest {

    private final TokenUsageEventListener listener = new TokenUsageEventListener();

    @Test
    @DisplayName("处理完整 Token 使用事件不抛异常")
    void handleTokenUsedEvent_fullEvent_noException() {
        TokenUsedEvent event = TokenUsedEvent.builder()
                .userId(1L)
                .apiKeyId(2L)
                .teamId(3L)
                .model("gpt-4")
                .provider("openai")
                .promptTokens(100)
                .completionTokens(50)
                .cost(BigDecimal.valueOf(0.01))
                .traceId("trace-1")
                .occurredOn(Instant.now())
                .build();

        assertThatCode(() -> listener.handleTokenUsedEvent(event)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("处理最小 Token 使用事件不抛异常")
    void handleTokenUsedEvent_minimalEvent_noException() {
        TokenUsedEvent event = TokenUsedEvent.builder()
                .userId(1L)
                .promptTokens(10)
                .completionTokens(5)
                .build();

        assertThatCode(() -> listener.handleTokenUsedEvent(event)).doesNotThrowAnyException();
    }
}
