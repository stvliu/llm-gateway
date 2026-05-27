package com.codingas.gateway.application.proxy.routing;

import com.codingas.gateway.common.exception.ResourceNotFoundException;
import com.codingas.gateway.domain.supply.entity.Channel;
import com.codingas.gateway.domain.supply.entity.ChannelModel;
import com.codingas.gateway.domain.supply.enums.ChannelState;
import com.codingas.gateway.domain.supply.gateway.ChannelGateway;
import com.codingas.gateway.domain.supply.gateway.ChannelModelGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

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

    private ChannelSelector selector;

    @BeforeEach
    void setUp() {
        selector = new ChannelSelector(channelModelGateway, channelGateway);
    }

    @Nested
    @DisplayName("select 方法")
    class SelectTests {

        @Test
        @DisplayName("有活跃通道时返回第一个活跃的 ChannelModel")
        void select_withActiveChannel_returnsFirstActive() {
            ChannelModel cm1 = mock(ChannelModel.class);
            when(cm1.getChannelId()).thenReturn(100L);
            ChannelModel cm2 = mock(ChannelModel.class);
            when(cm2.getChannelId()).thenReturn(200L);

            when(channelModelGateway.findActiveByModelSpecId(1L)).thenReturn(List.of(cm1, cm2));

            Channel ch1 = mock(Channel.class);
            when(ch1.getState()).thenReturn(ChannelState.INACTIVE);

            Channel ch2 = mock(Channel.class);
            when(ch2.getId()).thenReturn(200L);
            when(ch2.getState()).thenReturn(ChannelState.ACTIVE);

            when(channelGateway.findByIds(List.of(100L, 200L))).thenReturn(List.of(ch1, ch2));

            ChannelModel result = selector.select(1L);

            assertThat(result).isSameAs(cm2);
        }

        @Test
        @DisplayName("没有活跃通道时抛出 ResourceNotFoundException")
        void select_noActiveChannel_throwsException() {
            ChannelModel cm1 = mock(ChannelModel.class);
            when(cm1.getChannelId()).thenReturn(100L);

            when(channelModelGateway.findActiveByModelSpecId(1L)).thenReturn(List.of(cm1));

            Channel ch1 = mock(Channel.class);
            when(ch1.getState()).thenReturn(ChannelState.INACTIVE);

            when(channelGateway.findByIds(List.of(100L))).thenReturn(List.of(ch1));

            assertThatThrownBy(() -> selector.select(1L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("没有 ChannelModel 时抛出 ResourceNotFoundException")
        void select_noChannelModel_throwsException() {
            when(channelModelGateway.findActiveByModelSpecId(1L)).thenReturn(List.of());

            assertThatThrownBy(() -> selector.select(1L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}