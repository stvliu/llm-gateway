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
package com.codingas.gateway.protocol.transport;

import com.codingas.gateway.protocol.ProtocolRequest;
import com.codingas.gateway.protocol.ProtocolResponse;
import com.codingas.gateway.protocol.StreamCallback;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UpstreamClientRegistryImplTest {

    static class FakeFactory implements ProtocolUpstreamClientFactory {
        private final String protocol;
        FakeFactory(String protocol) { this.protocol = protocol; }
        @Override public String supportedProtocol() { return protocol; }
        @Override public UpstreamClient<? extends ProtocolRequest> create(String endpointUrl, String apiKey, int timeoutSeconds) {
            return new UpstreamClient<ProtocolRequest>() {
                @Override public ProtocolResponse chat(ProtocolRequest request) { return null; }
                @Override public void chatStream(ProtocolRequest request, StreamCallback callback) { }
                @Override public ConnectivityTestResult testConnectivity() { return null; }
                @Override public String supportedProvider() { return protocol; }
            };
        }
    }

    @Test
    void collectsFactoriesByProtocol() {
        UpstreamClientRegistry registry = new UpstreamClientRegistryImpl(
                List.of(new FakeFactory("openai"), new FakeFactory("anthropic")));
        assertThat(registry.getSupportedProtocols()).containsExactlyInAnyOrder("openai", "anthropic");
    }

    @Test
    void returnsClientForKnownProtocol() {
        UpstreamClientRegistry registry = new UpstreamClientRegistryImpl(List.of(new FakeFactory("openai")));
        UpstreamClient<ProtocolRequest> client = registry.getClient("openai", "http://x", "k", 30);
        assertThat(client.supportedProvider()).isEqualTo("openai");
    }

    @Test
    void rejectsUnknownProtocol() {
        UpstreamClientRegistry registry = new UpstreamClientRegistryImpl(List.of(new FakeFactory("openai")));
        assertThatThrownBy(() -> registry.getClient("gemini", "http://x", "k", 30))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("不支持的协议");
    }
}
