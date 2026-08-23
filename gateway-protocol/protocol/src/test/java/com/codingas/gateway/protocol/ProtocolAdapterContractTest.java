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
package com.codingas.gateway.protocol;

import com.codingas.gateway.protocol.canonical.CanonicalChatRequest;
import com.codingas.gateway.protocol.canonical.CanonicalChatResponse;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProtocolAdapterContractTest {

    /** 最小实现：identity 适配器，用于验证接口契约可被实现 */
    static class IdentityAdapter implements ProtocolAdapter<Object> {

        @Override
        public String protocol() {
            return "identity";
        }

        @Override
        public CanonicalChatRequest normalizeRequest(Object nativeReq) {
            return new CanonicalChatRequest();
        }

        @Override
        public Object denormalizeRequest(CanonicalChatRequest canonical) {
            return new Object();
        }

        @Override
        public CanonicalChatResponse normalizeResponse(Object nativeResp) {
            return new CanonicalChatResponse();
        }

        @Override
        public Object denormalizeResponse(CanonicalChatResponse canonical) {
            return new Object();
        }
    }

    @Test
    void spiIsImplementable() {
        ProtocolAdapter<Object> adapter = new IdentityAdapter();
        assertThat(adapter.protocol()).isEqualTo("identity");
        assertThat(adapter.normalizeRequest(new Object())).isNotNull();
        assertThat(adapter.denormalizeRequest(new CanonicalChatRequest())).isNotNull();
        assertThat(adapter.normalizeResponse(new Object())).isNotNull();
        assertThat(adapter.denormalizeResponse(new CanonicalChatResponse())).isNotNull();
    }
}
