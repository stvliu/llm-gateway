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
import com.codingas.gateway.provider.vendor.Provider;

import java.util.List;

/**
 * 渠道管理服务接口
 *
 * <p>出入参采用实体与轻量用例对象；跨实体的展示数据（提供商名称、端点列表）
 * 由本服务提供查询方法，web 层组装器（Assembler）经服务获取后组装 DTO。</p>
 */
public interface ChannelService {

    /**
     * 创建渠道
     *
     * @param channel 渠道实体（承载 providerId/name/billingMode 等）
     * @return 创建后的渠道实体
     */
    Channel create(Channel channel);

    /**
     * 更新渠道
     *
     * @param id      渠道 ID
     * @param channel 渠道实体
     * @return 更新后的渠道实体
     */
    Channel update(Long id, Channel channel);

    /**
     * 按 ID 获取渠道
     *
     * @param id 渠道 ID
     * @return 渠道实体
     */
    Channel getById(Long id);

    /**
     * 获取所有渠道列表（默认按渠道名升序）
     *
     * @return 渠道实体列表
     */
    List<Channel> getAll();

    /**
     * 获取所有渠道列表（支持字段排序）
     *
     * <p>排序字段白名单：name/providerId/state/id，非法字段回退默认 name。</p>
     *
     * @param sortBy    排序字段
     * @param sortOrder 排序方向（ASC/DESC）
     * @return 渠道实体列表
     */
    List<Channel> getAll(String sortBy, String sortOrder);

    /**
     * 按提供商 ID 获取渠道列表（默认按渠道名升序）
     *
     * @param providerId 提供商 ID
     * @return 渠道实体列表
     */
    List<Channel> getByProviderId(Long providerId);

    /**
     * 按提供商 ID 获取渠道列表（支持字段排序）
     *
     * @param providerId 提供商 ID
     * @param sortBy     排序字段（白名单同 {@link #getAll(String, String)}）
     * @param sortOrder  排序方向（ASC/DESC）
     * @return 渠道实体列表
     */
    List<Channel> getByProviderId(Long providerId, String sortBy, String sortOrder);

    /**
     * 按提供商 ID 与计费模式获取渠道列表（默认按渠道名升序）
     *
     * @param providerId  提供商 ID
     * @param billingMode 计费模式
     * @return 渠道实体列表
     */
    List<Channel> getByProviderIdAndBillingMode(Long providerId, BillingMode billingMode);

    /**
     * 按提供商 ID 与计费模式获取渠道列表（支持字段排序）
     *
     * @param providerId  提供商 ID
     * @param billingMode 计费模式
     * @param sortBy      排序字段（白名单同 {@link #getAll(String, String)}）
     * @param sortOrder   排序方向（ASC/DESC）
     * @return 渠道实体列表
     */
    List<Channel> getByProviderIdAndBillingMode(Long providerId, BillingMode billingMode,
                                                String sortBy, String sortOrder);

    /**
     * 按 ID 获取提供商（供展示组装：渠道响应需提供商名称）
     *
     * @param providerId 提供商 ID
     * @return 提供商实体（不存在时为 null）
     */
    Provider getProvider(Long providerId);

    /**
     * 按渠道 ID 获取端点列表（供展示组装：渠道响应需端点列表）
     *
     * @param channelId 渠道 ID
     * @return 端点实体列表
     */
    List<ChannelEndpoint> getEndpoints(Long channelId);

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
     * @param targetState 目标状态（ChannelState 枚举名）
     * @param reason      切换原因
     */
    void setState(Long id, String targetState, String reason);

    /**
     * 添加渠道端点
     *
     * @param endpoint 端点实体（承载 channelId/protocol/endpointUrl）
     * @return 添加后的端点实体
     */
    ChannelEndpoint addEndpoint(ChannelEndpoint endpoint);

    /**
     * 更新渠道端点
     *
     * @param channelId  渠道 ID
     * @param endpointId 端点 ID
     * @param command    端点用例入参
     * @return 更新后的端点实体
     */
    ChannelEndpoint updateEndpoint(Long channelId, Long endpointId, ChannelEndpoint endpoint);

    /**
     * 删除渠道端点
     *
     * @param channelId  渠道 ID
     * @param endpointId 端点 ID
     */
    void removeEndpoint(Long channelId, Long endpointId);

    /**
     * 复制渠道配置生成新渠道
     *
     * <p>基于源渠道复制本体配置（供应商/计费/配额/超时/重试），并复制其全部
     * 端点与模型实例（模型实例重置为 ACTIVE 可用状态）；新渠道状态重置为
     * ACTIVE、健康字段清空。凭证（API Key）仅在 {@code copyCredentials} 为
     * true 时复制（复用明文 Key 按既有加密策略重新加密），默认不复制。</p>
     *
     * @param sourceId        源渠道 ID
     * @param override        覆盖字段（name 必填非 null，其余继承源）
     * @param copyCredentials 是否同时复制凭证（API Key）
     * @return 新建渠道实体
     */
    Channel copy(Long sourceId, Channel override, boolean copyCredentials);
}
