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
package com.codingas.gateway.domain.supply.gateway;

import com.codingas.gateway.domain.supply.entity.Channel;
import com.codingas.gateway.domain.supply.enums.BillingMode;

import java.util.List;
import java.util.Optional;

/**
 * 渠道持久化接口
 *
 * <p>定义在 domain 层，由 infrastructure 层实现。</p>
 */
public interface ChannelGateway {

    /**
     * 保存渠道
     */
    Channel save(Channel channel);

    /**
     * 根据 ID 查找渠道
     */
    Optional<Channel> findById(Long id);

    /**
     * 根据供应商 ID 查找渠道
     */
    List<Channel> findByProviderId(Long providerId);

    /**
     * 查找活跃渠道
     */
    List<Channel> findAllActive();

    /**
     * 查询所有渠道
     */
    List<Channel> findAll();

    /**
     * 删除渠道
     */
    void deleteById(Long id);

    /**
     * 批量查找渠道
     */
    List<Channel> findByIds(List<Long> ids);

    /**
     * 检查供应商下是否存在同名渠道
     */
    boolean existsByProviderIdAndName(Long providerId, String name);

    /**
     * 根据供应商 ID 和名称查找渠道
     */
    Optional<Channel> findByProviderIdAndName(Long providerId, String name);

    /**
     * 根据供应商ID和计费模式查找渠道
     */
    List<Channel> findByProviderIdAndBillingMode(Long providerId, BillingMode billingMode);
}