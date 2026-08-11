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
package com.codingas.gateway.domain.supply.entity;

import com.codingas.gateway.domain.supply.enums.Protocol;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ChannelEndpoint 实体测试")
class ChannelEndpointTest {

    @Test
    @DisplayName("创建 ChannelEndpoint — 基本属性")
    void create_basicProperties() {
        ChannelEndpoint endpoint = new ChannelEndpoint();
        endpoint.setChannelId(1L);
        endpoint.setProtocol(Protocol.OPENAI);
        endpoint.setEndpointUrl("https://api.openai.com");

        assertThat(endpoint.getChannelId()).isEqualTo(1L);
        assertThat(endpoint.getProtocol()).isEqualTo(Protocol.OPENAI);
        assertThat(endpoint.getEndpointUrl()).isEqualTo("https://api.openai.com");
    }
}
