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
package com.codingas.gateway.integration;

import com.codingas.gateway.boot.GatewayApplication;
import com.codingas.gateway.proxy.conversion.OutboundTuner;
import com.codingas.gateway.protocol.raw.AnthropicMessagesRequest;
import com.codingas.gateway.protocol.raw.OpenAIChatRequest;
import com.codingas.gateway.protocol.Protocol;
import com.codingas.gateway.proxy.routing.RoutingContext;
import com.codingas.gateway.common.enums.FailureStrategy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 协议调谐器装配验证（validator/tuner 迁回插件后由插件 AutoConfiguration @Bean 注册）。
 *
 * <p>验证 {@link OutboundTuner} 按 {@code List<ProtocolTuner<?>>} 集合注入仍能收集到
 * openai/anthropic 两个 tuner 并生效（openai 补 4096、anthropic 补 1024）——
 * 若插件 @Bean 未注册或 boot 误扫导致重复/缺失，此处即回归。</p>
 */
@SpringBootTest(classes = GatewayApplication.class)
@ActiveProfiles("test")
@DisplayName("协议调谐器装配验证")
class ProtocolTunerWiringTest {

    @Autowired
    private OutboundTuner outboundTuner;

    @Test
    @DisplayName("openai 调谐器已收集：max_tokens 缺省补 4096")
    void openAiTuner_collectedAndApplied() {
        OpenAIChatRequest request = OpenAIChatRequest.builder()
                .model("gpt-4o")
                .messages(List.of(OpenAIChatRequest.Message.builder()
                        .role("user").content("hi").build()))
                .build();

        OpenAIChatRequest result = outboundTuner.tune(request, routingContext("openai", null));

        assertThat(result.getMaxTokens()).isEqualTo(4096);
    }

    @Test
    @DisplayName("anthropic 调谐器已收集：max_tokens 缺省补 1024")
    void anthropicTuner_collectedAndApplied() {
        AnthropicMessagesRequest request = AnthropicMessagesRequest.builder()
                .model("claude-sonnet-4")
                .messages(List.of(AnthropicMessagesRequest.Message.builder()
                        .role("user").content("hi").build()))
                .build();

        AnthropicMessagesRequest result = outboundTuner.tune(request, routingContext("anthropic", null));

        assertThat(result.getMaxTokens()).isEqualTo(1024);
    }

    /**
     * 构造路由上下文（模型名不做替换，聚焦协议级调谐）
     */
    private RoutingContext routingContext(String protocol, String upstreamModelName) {
        Protocol p = "anthropic".equals(protocol) ? Protocol.ANTHROPIC : Protocol.OPENAI;
        return new RoutingContext(
                10L, 20L, "https://api.example.com/v1",
                p, "sk-test", 60, false,
                "model-x", upstreamModelName,
                FailureStrategy.FAIL_RETRY
        );
    }
}
