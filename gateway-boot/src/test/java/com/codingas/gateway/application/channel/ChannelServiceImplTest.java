package com.codingas.gateway.application.channel;

import com.codingas.gateway.application.channel.dto.ChannelResponse;
import com.codingas.gateway.domain.supply.entity.Channel;
import com.codingas.gateway.domain.supply.enums.BillingMode;
import com.codingas.gateway.domain.supply.enums.ChannelState;
import com.codingas.gateway.domain.supply.gateway.ChannelCredentialGateway;
import com.codingas.gateway.domain.supply.gateway.ChannelEndpointGateway;
import com.codingas.gateway.domain.supply.gateway.ChannelGateway;
import com.codingas.gateway.domain.supply.gateway.ModelInstanceGateway;
import com.codingas.gateway.domain.supply.gateway.ProviderGateway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * ChannelServiceImpl 单元测试
 *
 * <p>验证 toResponse 对实体字段的透传，重点覆盖 clusterId（容灾总览页成员渠道映射依赖）。</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ChannelServiceImpl 测试")
class ChannelServiceImplTest {

    @Mock
    private ChannelGateway channelGateway;

    @Mock
    private ChannelEndpointGateway channelEndpointGateway;

    @Mock
    private ChannelCredentialGateway channelCredentialGateway;

    @Mock
    private ModelInstanceGateway modelInstanceGateway;

    @Mock
    private ProviderGateway providerGateway;

    @InjectMocks
    private ChannelServiceImpl channelService;

    @Test
    @DisplayName("getById 透传 channel.clusterId 到响应（容灾总览成员渠道映射依赖）")
    void getById_passesClusterIdToResponse() {
        Channel channel = buildChannel(1L, "ch-1");
        channel.setClusterId(42L);
        when(channelGateway.findById(1L)).thenReturn(Optional.of(channel));
        when(channelEndpointGateway.findByChannelId(1L)).thenReturn(List.of());

        ChannelResponse result = channelService.getById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getClusterId()).isEqualTo(42L);
    }

    @Test
    @DisplayName("getById 当 clusterId 为 null 时响应字段也为 null（向后兼容，未归属域的渠道）")
    void getById_nullClusterId_responseNull() {
        Channel channel = buildChannel(2L, "ch-2");
        // 不设置 clusterId，保持 null
        when(channelGateway.findById(2L)).thenReturn(Optional.of(channel));
        when(channelEndpointGateway.findByChannelId(2L)).thenReturn(List.of());

        ChannelResponse result = channelService.getById(2L);

        assertThat(result).isNotNull();
        assertThat(result.getClusterId()).isNull();
    }

    /** 构造最小可用渠道实体（state=ACTIVE，无端点） */
    private Channel buildChannel(Long id, String name) {
        Channel channel = new Channel();
        channel.setId(id);
        channel.setProviderId(10L);
        channel.setName(name);
        channel.setBillingMode(BillingMode.PAY_AS_YOU_GO);
        channel.setState(ChannelState.ACTIVE);
        return channel;
    }
}
