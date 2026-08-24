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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TokenUsedEvent 记录与 Builder 单元测试
 */
@DisplayName("TokenUsedEvent 测试")
class TokenUsedEventTest {

    @Test
    @DisplayName("builder 构建完整事件并返回全部字段")
    void builder_fullEvent_returnsAllFields() {
        Instant occurredOn = Instant.parse("2026-08-24T10:00:00Z");
        TokenUsedEvent event = TokenUsedEvent.builder()
                .userId(1L)
                .apiKeyId(2L)
                .teamId(3L)
                .model("gpt-4")
                .provider("openai")
                .promptTokens(100)
                .completionTokens(50)
                .cost(BigDecimal.valueOf(0.02))
                .traceId("trace-abc")
                .occurredOn(occurredOn)
                .build();

        assertThat(event.userId()).isEqualTo(1L);
        assertThat(event.apiKeyId()).isEqualTo(2L);
        assertThat(event.teamId()).isEqualTo(3L);
        assertThat(event.model()).isEqualTo("gpt-4");
        assertThat(event.provider()).isEqualTo("openai");
        assertThat(event.promptTokens()).isEqualTo(100);
        assertThat(event.completionTokens()).isEqualTo(50);
        assertThat(event.cost()).isEqualByComparingTo(BigDecimal.valueOf(0.02));
        assertThat(event.traceId()).isEqualTo("trace-abc");
        assertThat(event.occurredOn()).isEqualTo(occurredOn);
    }

    @Test
    @DisplayName("builder 默认值与 totalTokens 计算")
    void builder_defaultsAndTotalTokens() {
        TokenUsedEvent event = TokenUsedEvent.builder()
                .promptTokens(120)
                .completionTokens(30)
                .build();

        assertThat(event.cost()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(event.occurredOn()).isNotNull();
        assertThat(event.totalTokens()).isEqualTo(150);
    }

    @Test
    @DisplayName("toBuilder 保留原字段并支持覆盖")
    void toBuilder_preservesFieldsAndOverrides() {
        TokenUsedEvent original = TokenUsedEvent.builder()
                .userId(1L)
                .model("gpt-4")
                .promptTokens(100)
                .completionTokens(20)
                .build();

        TokenUsedEvent copied = original.toBuilder().model("claude-3").build();

        assertThat(copied.userId()).isEqualTo(1L);
        assertThat(copied.model()).isEqualTo("claude-3");
        assertThat(copied.promptTokens()).isEqualTo(100);
        assertThat(copied.completionTokens()).isEqualTo(20);
        // 原事件不被修改
        assertThat(original.model()).isEqualTo("gpt-4");
    }

    @Test
    @DisplayName("record 值语义：相等性基于字段")
    void record_equalsAndHashCode_valueSemantics() {
        TokenUsedEvent a = TokenUsedEvent.builder().userId(1L).promptTokens(10).completionTokens(2).build();
        TokenUsedEvent b = TokenUsedEvent.builder().userId(1L).promptTokens(10).completionTokens(2).build();
        TokenUsedEvent c = TokenUsedEvent.builder().userId(2L).promptTokens(10).completionTokens(2).build();

        assertThat(a).isEqualTo(b);
        assertThat(a).hasSameHashCodeAs(b);
        assertThat(a).isNotEqualTo(c);
        assertThat(a.toString()).contains("promptTokens");
    }
}
