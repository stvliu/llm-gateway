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
package com.codingas.gateway.application.protocol.conversion;

import com.codingas.gateway.domain.protocol.contract.AnthropicMessagesRequest;
import com.codingas.gateway.domain.protocol.contract.OpenAIChatRequest;
import com.codingas.gateway.infrastructure.protocol.AnthropicProtocolAdapter;
import com.codingas.gateway.infrastructure.protocol.OpenAIProtocolAdapter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProtocolConversionFacadeTest {

    @Test
    void convertRequestOpenaiToAnthropic() {
        ProtocolConversionFacade facade = new ProtocolConversionFacade(
                new OpenAIProtocolAdapter(new ObjectMapper()),
                new AnthropicProtocolAdapter());

        OpenAIChatRequest openai = OpenAIChatRequest.builder()
                .model("gpt-4o")
                .messages(List.of(OpenAIChatRequest.Message.builder().role("system").content("s").build(),
                        OpenAIChatRequest.Message.builder().role("user").content("hi").build()))
                .build();

        AnthropicMessagesRequest anthropic = (AnthropicMessagesRequest) facade.convertRequest(openai, "anthropic");

        assertThat(anthropic).isInstanceOf(AnthropicMessagesRequest.class);
        assertThat(anthropic.getSystem()).isEqualTo("s");
        assertThat(anthropic.getMessages()).hasSize(1);
    }

    @Test
    void convertRequestSameProtocolReturnsSame() {
        ProtocolConversionFacade facade = new ProtocolConversionFacade(
                new OpenAIProtocolAdapter(new ObjectMapper()),
                new AnthropicProtocolAdapter());

        OpenAIChatRequest openai = OpenAIChatRequest.builder().model("gpt-4o").build();

        assertThat(facade.convertRequest(openai, "openai")).isSameAs(openai);
    }
}
