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

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * OpenAIUpstreamClientFactory 单元测试。
 */
class OpenAIUpstreamClientFactoryTest {

    private final OpenAIUpstreamClientFactory factory =
            new OpenAIUpstreamClientFactory(new OkHttpClient(), new ObjectMapper());

    @Test
    void supportedProtocol_returnsOpenai() {
        assertThat(factory.supportedProtocol()).isEqualTo("openai");
    }

    @Test
    void create_returnsOpenAiUpstreamClient() {
        assertThat(factory.create("http://localhost:8080", "sk-test", 30))
                .isInstanceOf(OpenAIUpstreamClient.class);
    }
}
