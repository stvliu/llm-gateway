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
 *
 * <p>出入参采用领域实体与轻量用例对象，HTTP 契约（Request/Response DTO）由 web 层负责转换。</p>
 */
public interface ChannelService {

    /**
     * 创建渠道
     *
     * @param command 创建用例入参
     * @return 创建后的渠道实体
     */
    Channel create(ChannelCommand command);

    /**
     * 更新渠道
     *
     * @param id      渠道 ID
     * @param command 更新用例入参
     * @return 更新后的渠道实体
     */
    Channel update(Long id, ChannelCommand command);

    /**
     * 按 ID 获取渠道
     *
     * @param id 渠道 ID
     * @return 渠道实体
     */
    Channel getById(Long id);

    /**
     * 获取所有渠道列表
     *
     * @return 渠道实体列表
     */
    List<Channel> getAll();

    /**
     * 按提供商 ID 获取渠道列表
     *
     * @param providerId 提供商 ID
     * @return 渠道实体列表
     */
    List<Channel> getByProviderId(Long providerId);

    /**
     * 按提供商 ID 与计费模式获取渠道列表
     *
     * @param providerId  提供商 ID
     * @param billingMode 计费模式
     * @return 渠道实体列表
     */
    List<Channel> getByProviderIdAndBillingMode(Long providerId, BillingMode billingMode);

    /**
     * 删除渠道
     *
     * @param id 渠道 ID
     */
    void delete(Long id);

    /**
     * 切换渠道状态
     *
     * <p>由后端校验 canTransitionTo()，PENDING→ACTIVE 时校验前置条件并级联激活 ModelInstance。</p>
     *
     * @param id      渠道 ID
     * @param command 状态切换用例入参
     */
    void setState(Long id, ChannelStateCommand command);

    /**
     * 添加渠道端点
     *
     * @param command 端点用例入参
     * @return 添加后的端点实体
     */
    ChannelEndpoint addEndpoint(ChannelEndpointCommand command);

    /**
     * 更新渠道端点
     *
     * @param channelId  渠道 ID
     * @param endpointId 端点 ID
     * @param command    端点用例入参
     * @return 更新后的端点实体
     */
    ChannelEndpoint updateEndpoint(Long channelId, Long endpointId, ChannelEndpointCommand command);

    /**
     * 删除渠道端点
     *
     * @param channelId  渠道 ID
     * @param endpointId 端点 ID
     */
    void removeEndpoint(Long channelId, Long endpointId);
}
