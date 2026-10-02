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
package com.codingas.gateway.providerdata.model;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

/**
 * 模型实例 Repository
 */
@Repository
public interface ModelInstanceJpaRepository extends JpaRepository<ModelInstanceDo, Long> {

    List<ModelInstanceDo> findByChannelId(Long channelId);

    List<ModelInstanceDo> findByChannelIdAndState(Long channelId, String state);

    List<ModelInstanceDo> findByModelIdAndState(Long modelId, String state);

    List<ModelInstanceDo> findByModelId(Long modelId);

    List<ModelInstanceDo> findByModelIdAndStateOrderByPriorityAsc(Long modelId, String state);

    /**
     * 按渠道 + 可路由状态集合（ACTIVE/DEPRECATED）查询模型实例
     */
    List<ModelInstanceDo> findByChannelIdAndStateIn(Long channelId, List<String> states);

    /**
     * 按模型 + 可路由状态集合（ACTIVE/DEPRECATED）查询模型实例（按优先级升序）
     */
    List<ModelInstanceDo> findByModelIdAndStateInOrderByPriorityAsc(Long modelId, List<String> states);

    /**
     * 按模型 + 渠道 ID 集合 + 可路由状态集合（ACTIVE/DEPRECATED）查询模型实例（按优先级升序）
     *
     * <p>数据面路由前查询：渠道集合来自应用渠道配置（认证即授权下的配置读取），
     * DB 层过滤替代全量拉取 + 内存过滤。</p>
     *
     * @param modelId    模型 ID
     * @param channelIds 渠道 ID 集合（应用配置的可见渠道）
     * @param states     可路由状态集合（ACTIVE/DEPRECATED）
     * @return 活跃实例 DO 列表（priority 升序）
     */
    List<ModelInstanceDo> findByModelIdAndChannelIdInAndStateInOrderByPriorityAsc(
            Long modelId, Collection<Long> channelIds, List<String> states);

    List<ModelInstanceDo> findByIdIn(List<Long> ids);
}