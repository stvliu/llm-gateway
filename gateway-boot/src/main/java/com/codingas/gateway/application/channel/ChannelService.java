package com.codingas.gateway.application.channel;

import com.codingas.gateway.application.channel.dto.ChannelStateTransitionRequest;
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
     * 切换渠道状态
     *
     * <p>由后端校验 canTransitionTo()，PENDING→ACTIVE 时校验前置条件并级联激活 ModelInstance。</p>
     */
    void setState(Long id, ChannelStateTransitionRequest request);

    ChannelEndpointResponse addEndpoint(ChannelEndpointRequest request);

    ChannelEndpointResponse updateEndpoint(Long channelId, Long endpointId, ChannelEndpointRequest request);

    void removeEndpoint(Long channelId, Long endpointId);
}
