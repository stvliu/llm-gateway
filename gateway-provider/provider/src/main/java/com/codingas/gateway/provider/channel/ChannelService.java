/*
 * Copyright © 2025-2026 codingas.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.codingas.gateway.provider.channel;

import com.codingas.gateway.provider.model.BillingMode;

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
