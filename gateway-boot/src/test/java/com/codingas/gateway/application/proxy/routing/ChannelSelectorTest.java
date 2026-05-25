package com.codingas.gateway.application.proxy.routing;

import com.codingas.gateway.common.exception.ResourceNotFoundException;
import com.codingas.gateway.domain.supply.entity.Channel;
import com.codingas.gateway.domain.supply.entity.ChannelModel;
import com.codingas.gateway.domain.supply.enums.ChannelModelState;
import com.codingas.gateway.domain.supply.enums.ChannelState;
import com.codingas.gateway.domain.supply.gateway.ChannelGateway;
import com.codingas.gateway.domain.supply.gateway.ChannelModelGateway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * ChannelSelector 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ChannelSelector 单元测试")
class ChannelSelectorTest {

    @Mock
    private ChannelModelGateway channelModelGateway;

    @Mock
    private ChannelGateway channelGateway;

    @InjectMocks
    private ChannelSelector channelSelector;

    @Nested
    @DisplayName("select 通道选择")
    class SelectTests {

        @Test
        @DisplayName("选择成功 — 返回活跃通道的 ChannelModel")
        void select_activeChannel_returnsChannelModel() {
            // given
            ChannelModel channelModel = new ChannelModel();
            channelModel.setId(1L);
            channelModel.setChannelId(10L);
            channelModel.setModelSpecId(100L);
            channelModel.setState(ChannelModelState.ACTIVE);

            Channel channel = new Channel();
            channel.setId(10L);
            channel.setName("openai-main");
            channel.setState(ChannelState.ACTIVE);

            when(channelModelGateway.findActiveByModelSpecId(100L))
                    .thenReturn(List.of(channelModel));
            when(channelGateway.findById(10L))
                    .thenReturn(Optional.of(channel));

            // when
            ChannelModel result = channelSelector.select(100L);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getChannelId()).isEqualTo(10L);
            assertThat(result.getModelSpecId()).isEqualTo(100L);
        }

        @Test
        @DisplayName("无活跃通道时抛出 ResourceNotFoundException")
        void select_noActiveChannel_throwsException() {
            // given
            ChannelModel channelModel = new ChannelModel();
            channelModel.setId(1L);
            channelModel.setChannelId(10L);
            channelModel.setState(ChannelModelState.ACTIVE);

            Channel channel = new Channel();
            channel.setId(10L);
            channel.setState(ChannelState.DISABLED);

            when(channelModelGateway.findActiveByModelSpecId(100L))
                    .thenReturn(List.of(channelModel));
            when(channelGateway.findById(10L))
                    .thenReturn(Optional.of(channel));

            // when & then
            assertThatThrownBy(() -> channelSelector.select(100L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("ChannelModel")
                    .hasMessageContaining("100");
        }

        @Test
        @DisplayName("无 ChannelModel 记录时抛出 ResourceNotFoundException")
        void select_noChannelModels_throwsException() {
            // given
            when(channelModelGateway.findActiveByModelSpecId(999L))
                    .thenReturn(List.of());

            // when & then
            assertThatThrownBy(() -> channelSelector.select(999L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("ChannelModel")
                    .hasMessageContaining("999");
        }

        @Test
        @DisplayName("通道不存在时跳过该 ChannelModel")
        void select_channelNotFound_skipsModel() {
            // given
            ChannelModel channelModel = new ChannelModel();
            channelModel.setId(1L);
            channelModel.setChannelId(10L);
            channelModel.setState(ChannelModelState.ACTIVE);

            when(channelModelGateway.findActiveByModelSpecId(100L))
                    .thenReturn(List.of(channelModel));
            when(channelGateway.findById(10L))
                    .thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> channelSelector.select(100L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}
