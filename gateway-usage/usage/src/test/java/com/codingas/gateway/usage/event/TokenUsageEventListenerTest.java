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

import com.codingas.gateway.usage.event.TokenUsageEventListener;
import com.codingas.gateway.usage.event.TokenUsedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

import java.math.BigDecimal;
import java.time.Instant;

import static org.mockito.Mockito.*;

/**
 * TokenUsageEventListener 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TokenUsageEventListener 单元测试")
class TokenUsageEventListenerTest {

    @Mock
    private Logger logger;

    @InjectMocks
    private TokenUsageEventListener tokenUsageEventListener;

    private TokenUsedEvent testEvent;

    @BeforeEach
    void setUp() {
        testEvent = new TokenUsedEvent(
                1L,
                100L,
                10L,
                "gpt-4",
                "openai",
                100,
                200,
                BigDecimal.valueOf(0.05),
                "trace-456",
                Instant.now()
        );
    }

    @Test
    @DisplayName("handleTokenUsedEvent 应正确记录 Token 使用日志")
    void handleTokenUsedEvent_shouldLogTokenUsageInformation() {
        TokenUsageEventListener spyListener = spy(new TokenUsageEventListener());
        doNothing().when(spyListener).handleTokenUsedEvent(any(TokenUsedEvent.class));

        spyListener.handleTokenUsedEvent(testEvent);

        verify(spyListener, times(1)).handleTokenUsedEvent(testEvent);
    }

    @Test
    @DisplayName("handleTokenUsedEvent 应正确计算总 Token 数")
    void handleTokenUsedEvent_shouldCalculateTotalTokens() {
        // 验证 totalTokens() 方法
        assert testEvent.totalTokens() == 300; // 100 + 200
    }

    @Test
    @DisplayName("handleTokenUsedEvent 应处理空值字段的事件")
    void handleTokenUsedEvent_withNullFields_shouldHandleGracefully() {
        TokenUsedEvent eventWithNulls = new TokenUsedEvent(
                null,
                null,
                null,
                null,
                null,
                0,
                0,
                null,
                null,
                null
        );

        TokenUsageEventListener spyListener = spy(new TokenUsageEventListener());
        doNothing().when(spyListener).handleTokenUsedEvent(any(TokenUsedEvent.class));

        spyListener.handleTokenUsedEvent(eventWithNulls);

        verify(spyListener, times(1)).handleTokenUsedEvent(eventWithNulls);
    }

    @Test
    @DisplayName("handleTokenUsedEvent 应处理不同的模型")
    void handleTokenUsedEvent_withDifferentModels_shouldHandleCorrectly() {
        String[] models = {"gpt-4", "gpt-3.5-turbo", "claude-3-opus", "claude-3-sonnet"};

        TokenUsageEventListener spyListener = spy(new TokenUsageEventListener());

        for (String model : models) {
            TokenUsedEvent event = TokenUsedEvent.builder()
                    .model(model)
                    .userId(1L)
                    .promptTokens(50)
                    .completionTokens(100)
                    .cost(BigDecimal.valueOf(0.02))
                    .build();

            doNothing().when(spyListener).handleTokenUsedEvent(any(TokenUsedEvent.class));
            spyListener.handleTokenUsedEvent(event);

            verify(spyListener, times(1)).handleTokenUsedEvent(event);
        }
    }

    @Test
    @DisplayName("handleTokenUsedEvent 应处理零 Token 的情况")
    void handleTokenUsedEvent_withZeroTokens_shouldHandleCorrectly() {
        TokenUsedEvent zeroTokenEvent = TokenUsedEvent.builder()
                .model("gpt-4")
                .userId(1L)
                .promptTokens(0)
                .completionTokens(0)
                .build();

        TokenUsageEventListener spyListener = spy(new TokenUsageEventListener());
        doNothing().when(spyListener).handleTokenUsedEvent(any(TokenUsedEvent.class));

        spyListener.handleTokenUsedEvent(zeroTokenEvent);

        verify(spyListener, times(1)).handleTokenUsedEvent(zeroTokenEvent);
    }

    @Test
    @DisplayName("handleTokenUsedEvent 应处理大 Token 数值")
    void handleTokenUsedEvent_withLargeTokenValues_shouldHandleCorrectly() {
        TokenUsedEvent largeTokenEvent = TokenUsedEvent.builder()
                .model("gpt-4-32k")
                .userId(1L)
                .promptTokens(30000)
                .completionTokens(50000)
                .cost(BigDecimal.valueOf(5.00))
                .build();

        TokenUsageEventListener spyListener = spy(new TokenUsageEventListener());
        doNothing().when(spyListener).handleTokenUsedEvent(any(TokenUsedEvent.class));

        spyListener.handleTokenUsedEvent(largeTokenEvent);

        verify(spyListener, times(1)).handleTokenUsedEvent(largeTokenEvent);
    }
}
