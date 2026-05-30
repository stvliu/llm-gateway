package com.codingas.gateway.domain.supply.entity;

import com.codingas.gateway.domain.supply.enums.ChannelEndpointState;
import com.codingas.gateway.domain.supply.enums.Protocol;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ChannelEndpoint 实体测试")
class ChannelEndpointTest {

    @Test
    @DisplayName("创建 ChannelEndpoint — 默认 ACTIVE")
    void create_defaultState() {
        ChannelEndpoint endpoint = new ChannelEndpoint();
        endpoint.setChannelId(1L);
        endpoint.setProtocol(Protocol.OPENAI);
        endpoint.setEndpointUrl("https://api.openai.com");

        assertThat(endpoint.getChannelId()).isEqualTo(1L);
        assertThat(endpoint.getProtocol()).isEqualTo(Protocol.OPENAI);
        assertThat(endpoint.getEndpointUrl()).isEqualTo("https://api.openai.com");
        assertThat(endpoint.getState()).isEqualTo(ChannelEndpointState.ACTIVE);
    }

    @Test
    @DisplayName("isAvailable — ACTIVE 返回 true")
    void isAvailable_active() {
        ChannelEndpoint endpoint = new ChannelEndpoint();
        endpoint.setState(ChannelEndpointState.ACTIVE);
        assertThat(endpoint.isAvailable()).isTrue();
    }

    @Test
    @DisplayName("isAvailable — INACTIVE 返回 false")
    void isAvailable_disabled() {
        ChannelEndpoint endpoint = new ChannelEndpoint();
        endpoint.setState(ChannelEndpointState.INACTIVE);
        assertThat(endpoint.isAvailable()).isFalse();
    }

    @Test
    @DisplayName("禁用端点")
    void disable() {
        ChannelEndpoint endpoint = new ChannelEndpoint();
        endpoint.setState(ChannelEndpointState.ACTIVE);
        endpoint.disable();
        assertThat(endpoint.getState()).isEqualTo(ChannelEndpointState.INACTIVE);
        assertThat(endpoint.isAvailable()).isFalse();
    }

    @Test
    @DisplayName("启用端点")
    void enable() {
        ChannelEndpoint endpoint = new ChannelEndpoint();
        endpoint.setState(ChannelEndpointState.INACTIVE);
        endpoint.enable();
        assertThat(endpoint.getState()).isEqualTo(ChannelEndpointState.ACTIVE);
        assertThat(endpoint.isAvailable()).isTrue();
    }
}
