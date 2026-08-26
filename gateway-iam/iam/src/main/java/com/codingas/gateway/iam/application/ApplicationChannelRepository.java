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
package com.codingas.gateway.iam.application;

import com.codingas.gateway.iam.application.ApplicationChannel;

import java.util.List;
import java.util.Set;

/**
 * 应用-渠道授权关联领域网关接口
 *
 * <p>决定应用可见的渠道集合。domain 层仅依赖此接口，
 * 实现位于 infrastructure 层（COLA Light 依赖倒置）。</p>
 */
public interface ApplicationChannelRepository {

    /**
     * 查询应用可见的渠道 ID 集合（去重）
     *
     * @param appId 应用 ID
     * @return 渠道 ID 集合
     */
    Set<Long> findChannelIdsByApplicationId(Long appId);

    /**
     * 查询应用下的全部授权关联
     *
     * @param appId 应用 ID
     * @return 关联列表
     */
    List<ApplicationChannel> findByApplicationId(Long appId);

    /**
     * 批量保存授权关联
     *
     * @param rels 关联列表
     */
    void saveAll(List<ApplicationChannel> rels);

    /**
     * 删除应用下的全部授权关联
     *
     * <p>用于更新渠道授权时先清空旧关联，再批量保存新关联。</p>
     *
     * @param appId 应用 ID
     */
    void deleteByApplicationId(Long appId);
}
