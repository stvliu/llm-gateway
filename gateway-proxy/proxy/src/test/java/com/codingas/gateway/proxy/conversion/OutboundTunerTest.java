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
package com.codingas.gateway.proxy.conversion;

import com.codingas.gateway.domain.protocol.contract.OpenAIChatRequest;
import com.codingas.gateway.provider.upstream.Protocol;
import com.codingas.gateway.provider.upstream.RoutingContext;
import com.codingas.gateway.common.enums.FailureStrategy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * OutboundTuner 单元测试
 *
 * <p>验证出站调谐器的两层调谐逻辑：协议级调谐和渠道级模型名替换。</p>
 */
@DisplayName("OutboundTuner 单元测试")
class OutboundTunerTest {

    private final OutboundTuner outboundTuner = new OutboundTuner(List.of());

    @Nested
    @DisplayName("模型名替换")
    class ModelNameReplacementTests {

        @Test
        @DisplayName("upstreamModelName 不为 null 时替换模型名")
        void shouldReplaceModelNameWhenUpstreamModelNameIsNotNull() {
            // given
            OpenAIChatRequest request = OpenAIChatRequest.builder()
                    .model("gpt-4o")
                    .build();

            RoutingContext context = new RoutingContext(
                    10L, 20L, "https://api.openai.com/v1",
                    Protocol.OPENAI, "sk-test", 60, false,
                    "gpt-4o", "gpt-4o-upstream",
                    FailureStrategy.FAIL_RETRY
            );

            // when
            OpenAIChatRequest result = outboundTuner.tune(request, context);

            // then
            assertThat(result.getModel()).isEqualTo("gpt-4o-upstream");
        }

        @Test
        @DisplayName("upstreamModelName 为 null 时保留原模型名")
        void shouldKeepModelNameWhenUpstreamModelNameIsNull() {
            // given
            OpenAIChatRequest request = OpenAIChatRequest.builder()
                    .model("gpt-4o")
                    .build();

            RoutingContext context = new RoutingContext(
                    10L, 20L, "https://api.openai.com/v1",
                    Protocol.OPENAI, "sk-test", 60, false,
                    "gpt-4o", null,
                    FailureStrategy.FAIL_RETRY
            );

            // when
            OpenAIChatRequest result = outboundTuner.tune(request, context);

            // then
            assertThat(result.getModel()).isEqualTo("gpt-4o");
        }

        @Test
        @DisplayName("upstreamModelName 为空白时保留原模型名")
        void shouldKeepModelNameWhenUpstreamModelNameIsBlank() {
            // given
            OpenAIChatRequest request = OpenAIChatRequest.builder()
                    .model("gpt-4o")
                    .build();

            RoutingContext context = new RoutingContext(
                    10L, 20L, "https://api.openai.com/v1",
                    Protocol.OPENAI, "sk-test", 60, false,
                    "gpt-4o", "   ",
                    FailureStrategy.FAIL_RETRY
            );

            // when
            OpenAIChatRequest result = outboundTuner.tune(request, context);

            // then
            assertThat(result.getModel()).isEqualTo("gpt-4o");
        }
    }
}