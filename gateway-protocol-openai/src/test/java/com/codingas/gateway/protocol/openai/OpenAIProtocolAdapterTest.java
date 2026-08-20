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

import com.codingas.gateway.api.capability.protocol.CanonicalChatRequest;
import com.codingas.gateway.api.capability.protocol.CanonicalMessage;
import com.codingas.gateway.api.capability.protocol.CanonicalTool;
import com.codingas.gateway.domain.protocol.contract.OpenAIChatRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class OpenAIProtocolAdapterTest {

    private OpenAIProtocolAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new OpenAIProtocolAdapter(new ObjectMapper());
    }

    @Test
    void protocolReturnsOpenai() {
        assertThat(adapter.protocol()).isEqualTo("openai");
    }

    @Test
    void normalizeRequestExtractsSystemAndTool() {
        ObjectNode params = new ObjectMapper().createObjectNode();
        params.put("type", "object");
        OpenAIChatRequest.Message sys = OpenAIChatRequest.Message.builder()
                .role("system").content("你是助手").build();
        OpenAIChatRequest.Message user = OpenAIChatRequest.Message.builder()
                .role("user").content("hi").build();
        OpenAIChatRequest req = OpenAIChatRequest.builder()
                .model("gpt-4o")
                .messages(List.of(sys, user))
                .tools(List.of(java.util.Map.of("type", "function",
                        "function", java.util.Map.of("name", "f1", "parameters", params))))
                .stream(true)
                .build();

        CanonicalChatRequest c = adapter.normalizeRequest(req);

        assertThat(c.getSystem()).isEqualTo("你是助手");
        assertThat(c.getMessages()).hasSize(1);
        assertThat(c.getMessages().get(0).getRole()).isEqualTo("user");
        assertThat(c.getTools()).hasSize(1);
        assertThat(c.getTools().get(0).getName()).isEqualTo("f1");
        assertThat(c.isStream()).isTrue();
    }

    @Test
    void denormalizeRequestRoundTripsMessages() {
        CanonicalChatRequest c = CanonicalChatRequest.builder()
                .model("gpt-4o")
                .system("sys")
                .messages(List.of(CanonicalMessage.builder().role("user").content("hi").build()))
                .build();

        OpenAIChatRequest nativeReq = adapter.denormalizeRequest(c);

        assertThat(nativeReq.getModel()).isEqualTo("gpt-4o");
        assertThat(nativeReq.getMessages()).hasSize(2); // system 角色 + user
        assertThat(nativeReq.getMessages().get(0).getRole()).isEqualTo("system");
    }

    @Test
    void denormalizeRequestToleratesToolWithoutParameters() {
        // 工具无入参 schema（parameters=null）时 denormalizeRequest 不得抛 NPE
        CanonicalChatRequest c = CanonicalChatRequest.builder()
                .model("gpt-4o")
                .tools(List.of(CanonicalTool.builder()
                        .name("f1")
                        .description(null)
                        .parameters(null)
                        .build()))
                .build();

        OpenAIChatRequest nativeReq = adapter.denormalizeRequest(c);

        assertThat(nativeReq.getTools()).hasSize(1);
        Map<String, Object> tool = nativeReq.getTools().get(0);
        assertThat(tool.get("type")).isEqualTo("function");
        @SuppressWarnings("unchecked")
        Map<String, Object> function = (Map<String, Object>) tool.get("function");
        assertThat(function.get("name")).isEqualTo("f1");
        assertThat(function.containsKey("description")).isTrue(); // null 值保留，由序列化层省略
        assertThat(function.get("parameters")).isNull();
    }
}
