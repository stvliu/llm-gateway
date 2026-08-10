/*
 * Copyright (c) 2025 codingas.com
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE file for details.
 */
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
 * <p>验证 toResponse 对实体基础字段的透传。</p>
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
    @DisplayName("getById 透传渠道基础字段到响应")
    void getById_passesBasicFieldsToResponse() {
        Channel channel = buildChannel(1L, "ch-1");
        when(channelGateway.findById(1L)).thenReturn(Optional.of(channel));
        when(channelEndpointGateway.findByChannelId(1L)).thenReturn(List.of());

        ChannelResponse result = channelService.getById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("ch-1");
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
