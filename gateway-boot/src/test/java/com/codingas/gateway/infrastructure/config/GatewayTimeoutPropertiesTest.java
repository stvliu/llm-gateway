package com.codingas.gateway.infrastructure.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * GatewayTimeoutProperties 单元测试
 */
@DisplayName("分级超时配置测试")
class GatewayTimeoutPropertiesTest {

    @Nested
    @DisplayName("默认值")
    class DefaultValues {

        @Test
        @DisplayName("默认超时为 60 秒")
        void defaultTimeout_is60Seconds() {
            GatewayTimeoutProperties props = new GatewayTimeoutProperties();
            assertThat(props.getDefaultTimeoutSeconds()).isEqualTo(60);
        }

        @Test
        @DisplayName("未配置覆盖时返回默认值")
        void noOverride_returnsDefault() {
            GatewayTimeoutProperties props = new GatewayTimeoutProperties();
            assertThat(props.getTimeoutForChannel(100L)).isEqualTo(60);
            assertThat(props.getTimeoutForEndpoint(200L)).isEqualTo(60);
        }

        @Test
        @DisplayName("null 参数时返回默认值")
        void nullParam_returnsDefault() {
            GatewayTimeoutProperties props = new GatewayTimeoutProperties();
            assertThat(props.getTimeoutForChannel(null)).isEqualTo(60);
            assertThat(props.getTimeoutForEndpoint(null)).isEqualTo(60);
        }
    }

    @Nested
    @DisplayName("通道级覆盖")
    class ChannelOverrides {

        @Test
        @DisplayName("指定通道 ID 返回覆盖值")
        void channelOverride_returnsConfiguredValue() {
            GatewayTimeoutProperties props = new GatewayTimeoutProperties();
            props.setChannelTimeouts(Map.of("100", 120, "200", 30));

            assertThat(props.getTimeoutForChannel(100L)).isEqualTo(120);
            assertThat(props.getTimeoutForChannel(200L)).isEqualTo(30);
        }

        @Test
        @DisplayName("未覆盖的通道 ID 返回默认值")
        void uncoveredChannel_returnsDefault() {
            GatewayTimeoutProperties props = new GatewayTimeoutProperties();
            props.setChannelTimeouts(Map.of("100", 120));

            assertThat(props.getTimeoutForChannel(999L)).isEqualTo(60);
        }
    }

    @Nested
    @DisplayName("端点级覆盖")
    class EndpointOverrides {

        @Test
        @DisplayName("指定端点 ID 返回覆盖值")
        void endpointOverride_returnsConfiguredValue() {
            GatewayTimeoutProperties props = new GatewayTimeoutProperties();
            props.setEndpointTimeouts(Map.of("300", 90));

            assertThat(props.getTimeoutForEndpoint(300L)).isEqualTo(90);
        }
    }
}