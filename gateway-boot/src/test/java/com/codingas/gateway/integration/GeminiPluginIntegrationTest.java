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
import com.codingas.gateway.protocol.ProtocolAdapter;
import com.codingas.gateway.proxy.conversion.ProtocolConversionFacade;
import com.codingas.gateway.protocol.contract.OpenAIChatRequest;
import com.codingas.gateway.protocol.gemini.GeminiChatRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Gemini 协议插件可扩展性验证（验收金标准）。
 *
 * <p>验证新增协议（gemini）仅通过注册插件 Adapter（AutoConfiguration），
 * 即可被 {@link ProtocolConversionFacade} 按协议名通用路由转换，
 * 无需改动 gateway-proxy / gateway-protocol 核心。</p>
 */
@SpringBootTest(classes = GatewayApplication.class)
@ActiveProfiles("test")
@DisplayName("Gemini 协议插件集成")
class GeminiPluginIntegrationTest {

    @Autowired
    private ProtocolConversionFacade facade;

    @Autowired
    private List<ProtocolAdapter<?>> adapters;

    @Test
    @DisplayName("gemini 协议插件已注册为 ProtocolAdapter Bean")
    void geminiAdapter_registered() {
        List<String> protocols = adapters.stream().map(ProtocolAdapter::protocol).collect(Collectors.toList());
        assertThat(protocols).contains("gemini", "openai", "anthropic");
    }

    @Test
    @DisplayName("gemini 请求可经 Facade 通用路由转换为 openai")
    void geminiToOpenAI_convertsViaSpi() {
        GeminiChatRequest gemini = new GeminiChatRequest();
        gemini.setModel("gemini-1.5-pro");
        gemini.setSystem("s");
        gemini.addMessage(new GeminiChatRequest.Message("user", "hi"));

        OpenAIChatRequest openai = (OpenAIChatRequest) facade.convertRequest(gemini, "openai");

        assertThat(openai).isNotNull();
        assertThat(openai.getModel()).isEqualTo("gemini-1.5-pro");
        // gemini normalize 后 system → openai denormalize 还原为 system 消息
        assertThat(openai.getMessages()).anyMatch(m -> "system".equals(m.getRole()) && "s".equals(m.getContent()));
        assertThat(openai.getMessages()).anyMatch(m -> "user".equals(m.getRole()) && "hi".equals(m.getContent()));
    }
}
