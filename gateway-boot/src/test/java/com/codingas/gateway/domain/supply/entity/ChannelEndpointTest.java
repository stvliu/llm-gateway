/*
 * Copyright (c) 2025 codingas.com
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE file for details.
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
