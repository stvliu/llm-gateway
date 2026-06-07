package com.codingas.gateway.application.channel;

import com.codingas.gateway.application.channel.dto.ChannelEndpointRequest;
import com.codingas.gateway.application.channel.dto.ChannelEndpointResponse;
import com.codingas.gateway.application.channel.dto.ChannelRequest;
import com.codingas.gateway.application.channel.dto.ChannelResponse;
import com.codingas.gateway.domain.supply.enums.BillingMode;

import java.util.List;

/**
 * 渠道应用服务接口
 */
public interface ChannelService {

    ChannelResponse create(ChannelRequest request);

    ChannelResponse update(Long id, ChannelRequest request);

    ChannelResponse getById(Long id);

    /**
     * 获取所有渠道列表
     */
    List<ChannelResponse> getAll();

    List<ChannelResponse> getByProviderId(Long providerId);

    List<ChannelResponse> getByProviderIdAndBillingMode(Long providerId, BillingMode billingMode);

    void delete(Long id);

    /**
     * 切换渠道启用/停用状态
     */
    void setState(Long id, boolean enabled);

    ChannelEndpointResponse addEndpoint(Long channelId, ChannelEndpointRequest request);

    ChannelEndpointResponse updateEndpoint(Long channelId, Long endpointId, ChannelEndpointRequest request);

    void removeEndpoint(Long channelId, Long endpointId);
}
