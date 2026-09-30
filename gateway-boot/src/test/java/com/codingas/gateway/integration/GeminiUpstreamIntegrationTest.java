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
import com.codingas.gateway.proxy.conversion.ProtocolConversionFacade;
import com.codingas.gateway.protocol.ProtocolRequest;
import com.codingas.gateway.protocol.ProtocolResponse;
import com.codingas.gateway.protocol.gemini.GeminiChatRequest;
import com.codingas.gateway.protocol.gemini.GeminiChatResponse;
import com.codingas.gateway.protocol.raw.AnthropicMessagesRequest;
import com.codingas.gateway.protocol.raw.OpenAIChatRequest;
import com.codingas.gateway.protocol.raw.OpenAIChatResponse;
import com.codingas.gateway.protocol.transport.UpstreamClient;
import com.codingas.gateway.protocol.transport.UpstreamClientRegistry;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Gemini 出站协议端到端集成测试（目标：OpenAI/Anthropic 入站 → Gemini 出站）。
 *
 * <p>使用完整 Spring 上下文中的真实组件（{@link UpstreamClientRegistry} + {@link ProtocolConversionFacade}），
 * MockWebServer 模拟 Gemini 上游，验证三处断链点（传输层注册 / 非流式响应转换 / 请求转换）已全部接通：
 * OpenAI/Anthropic 入站请求 → Facade 转换 → GeminiUpstreamClient 出站 → 响应转换回入站协议格式。</p>
 */
@SpringBootTest(classes = GatewayApplication.class)
@ActiveProfiles("test")
@DisplayName("Gemini 出站协议端到端集成")
class GeminiUpstreamIntegrationTest {

    @Autowired
    private UpstreamClientRegistry clientRegistry;

    @Autowired
    private ProtocolConversionFacade conversionFacade;

    private MockWebServer geminiServer;

    @BeforeEach
    void setUp() throws IOException {
        geminiServer = new MockWebServer();
        geminiServer.start();
    }

    @AfterEach
    void tearDown() throws IOException {
        geminiServer.shutdown();
    }

    private String baseUrl() {
        return geminiServer.url("").toString().replaceAll("/$", "");
    }

    private static String geminiSuccessBody() {
        return """
                {
                  "candidates": [
                    {
                      "content": {"parts": [{"text": "Hello from Gemini"}], "role": "model"},
                      "finishReason": "STOP",
                      "index": 0
                    }
                  ],
                  "usageMetadata": {
                    "promptTokenCount": 10,
                    "candidatesTokenCount": 8,
                    "totalTokenCount": 18
                  },
                  "modelVersion": "gemini-2.5-pro"
                }""";
    }

    @Test
    @DisplayName("registry 支持 gemini 协议（Factory 已注册并接入 SessionStart hook 装配）")
    void registry_supportsGemini() {
        assertThat(clientRegistry.getSupportedProtocols()).contains("gemini", "openai", "anthropic");
    }

    @Test
    @DisplayName("OpenAI 入站 → Gemini 出站 → OpenAI 响应（非流式全链路）")
    void openaiInbound_geminiOutbound_returnsOpenAIResponse() {
        geminiServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(geminiSuccessBody()));

        // ① 入站 OpenAI 请求 → 规范 → Gemini 出站请求（Facade 转换）
        OpenAIChatRequest openaiReq = OpenAIChatRequest.builder()
                .model("gemini-2.5-pro")
                .messages(List.of(new OpenAIChatRequest.Message("user", "Hello", null, null, null)))
                .build();
        ProtocolRequest geminiReq = conversionFacade.convertRequest(openaiReq, "gemini");
        assertThat(geminiReq).isInstanceOf(GeminiChatRequest.class);
        assertThat(geminiReq.getProtocol()).isEqualTo("gemini");

        // ③ 经注册表获取 Gemini 出站客户端并调用（真实传输）
        UpstreamClient<ProtocolRequest> client = clientRegistry.getClient("gemini", baseUrl(), "gem-key", 30);
        ProtocolResponse geminiResp = client.chat(geminiReq);
        assertThat(geminiResp).isInstanceOf(GeminiChatResponse.class);
        assertThat(((GeminiChatResponse) geminiResp).text()).isEqualTo("Hello from Gemini");

        // ④ 响应转换回 OpenAI 格式（通用 Adapter 路由）
        ProtocolResponse openaiResp = conversionFacade.convertResponse(geminiResp, "gemini", "openai");
        assertThat(openaiResp).isInstanceOf(OpenAIChatResponse.class);
        OpenAIChatResponse result = (OpenAIChatResponse) openaiResp;
        assertThat(result.getModel()).isEqualTo("gemini-2.5-pro");
        assertThat(result.getChoices()).hasSize(1);
        assertThat(result.getChoices().get(0).getMessage().getContent()).isEqualTo("Hello from Gemini");
        assertThat(result.getUsage().getPromptTokens()).isEqualTo(10);
        assertThat(result.getUsage().getCompletionTokens()).isEqualTo(8);
    }

    @Test
    @DisplayName("OpenAI 入站多轮（含 assistant 历史）→ Gemini 请求：assistant 归一为 model")
    void openaiInbound_multiTurn_rolesNormalizedToGemini() {
        OpenAIChatRequest openaiReq = OpenAIChatRequest.builder()
                .model("gemini-2.5-pro")
                .messages(List.of(
                        new OpenAIChatRequest.Message("user", "Hi", null, null, null),
                        new OpenAIChatRequest.Message("assistant", "Hello!", null, null, null),
                        new OpenAIChatRequest.Message("user", "Again", null, null, null)))
                .build();

        GeminiChatRequest gemini = (GeminiChatRequest) conversionFacade.convertRequest(openaiReq, "gemini");

        assertThat(gemini.getMessages()).extracting(GeminiChatRequest.Message::role)
                .containsExactly("user", "model", "user");
        assertThat(gemini.getMessages().get(1).content()).isEqualTo("Hello!");
    }

    @Test
    @DisplayName("Anthropic 入站 → Gemini 出站请求转换（request 方向打通）")
    void anthropicInbound_convertsToGeminiRequest() {
        AnthropicMessagesRequest anthropicReq = AnthropicMessagesRequest.builder()
                .model("gemini-2.5-pro")
                .messages(List.of(AnthropicMessagesRequest.Message.builder()
                        .role("user")
                        .content("Hello")
                        .build()))
                .build();

        ProtocolRequest geminiReq = conversionFacade.convertRequest(anthropicReq, "gemini");

        assertThat(geminiReq).isInstanceOf(GeminiChatRequest.class);
        GeminiChatRequest gemini = (GeminiChatRequest) geminiReq;
        assertThat(gemini.getModel()).isEqualTo("gemini-2.5-pro");
        assertThat(gemini.getMessages()).hasSize(1);
        assertThat(gemini.getMessages().get(0).content()).isEqualTo("Hello");
    }
}
