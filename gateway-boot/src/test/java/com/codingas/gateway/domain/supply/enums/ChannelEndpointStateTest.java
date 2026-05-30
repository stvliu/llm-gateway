package com.codingas.gateway.domain.supply.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ChannelEndpointState 枚举测试")
class ChannelEndpointStateTest {

    @Test
    @DisplayName("fromCode — 正常值")
    void fromCode_valid() {
        assertThat(ChannelEndpointState.fromCode("ACTIVE")).isEqualTo(ChannelEndpointState.ACTIVE);
        assertThat(ChannelEndpointState.fromCode("inactive")).isEqualTo(ChannelEndpointState.INACTIVE);
    }

    @Test
    @DisplayName("fromCode — 大小写不敏感")
    void fromCode_caseInsensitive() {
        assertThat(ChannelEndpointState.fromCode("active")).isEqualTo(ChannelEndpointState.ACTIVE);
    }

    @Test
    @DisplayName("fromCode — 无效值抛异常")
    void fromCode_invalid() {
        assertThatThrownBy(() -> ChannelEndpointState.fromCode("INVALID"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("isAvailable — ACTIVE 返回 true")
    void isAvailable_active() {
        assertThat(ChannelEndpointState.ACTIVE.isAvailable()).isTrue();
    }

    @Test
    @DisplayName("isAvailable — INACTIVE 返回 false")
    void isAvailable_disabled() {
        assertThat(ChannelEndpointState.INACTIVE.isAvailable()).isFalse();
    }
}
