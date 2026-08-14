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
package com.codingas.gateway.infrastructure.protocol;

import com.codingas.gateway.api.capability.protocol.CanonicalChatRequest;
import com.codingas.gateway.api.capability.protocol.CanonicalMessage;
import com.codingas.gateway.domain.protocol.contract.AnthropicMessagesRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AnthropicProtocolAdapterTest {

    private AnthropicProtocolAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new AnthropicProtocolAdapter();
    }

    @Test
    void protocolReturnsAnthropic() {
        assertThat(adapter.protocol()).isEqualTo("anthropic");
    }

    @Test
    void normalizeRequestKeepsTopLevelSystem() {
        AnthropicMessagesRequest req = AnthropicMessagesRequest.builder()
                .model("claude-4")
                .system("你是助手")
                .messages(List.of(AnthropicMessagesRequest.Message.builder()
                        .role("user").content("hi").build()))
                .build();

        CanonicalChatRequest c = adapter.normalizeRequest(req);

        assertThat(c.getSystem()).isEqualTo("你是助手");
        assertThat(c.getMessages()).hasSize(1);
        assertThat(c.getMessages().get(0).getRole()).isEqualTo("user");
    }

    @Test
    void denormalizeRequestRestoresSystem() {
        CanonicalChatRequest c = CanonicalChatRequest.builder()
                .model("claude-4")
                .system("sys")
                .messages(List.of(CanonicalMessage.builder().role("user").content("hi").build()))
                .build();

        AnthropicMessagesRequest req = adapter.denormalizeRequest(c);

        assertThat(req.getSystem()).isEqualTo("sys");
        assertThat(req.getMessages()).hasSize(1);
        assertThat(req.getStopSequences()).isNull();
    }
}
