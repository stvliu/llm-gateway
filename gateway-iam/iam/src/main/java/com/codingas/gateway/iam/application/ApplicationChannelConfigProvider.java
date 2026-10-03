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

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 应用渠道配置提供者（本地缓存装饰）
 *
 * <p>数据面路由前配置读取（应用→渠道集合、应用→渠道优先级映射）为每请求热点查询，
 * 以 Caffeine 本地缓存承载：显式失效为主（配置写操作即时 evict）、TTL 60 秒兜底防遗漏。
 * applicationId 为 null 不缓存（直接返回空集/空映射，语义与现状一致）。</p>
 */
@Service
public class ApplicationChannelConfigProvider {

    /** 应用 → 可见渠道集合缓存 */
    private final Cache<Long, Set<Long>> channelIdsCache;
    /** 应用 → 渠道优先级映射缓存 */
    private final Cache<Long, Map<Long, Integer>> priorityMapCache;

    private final ApplicationChannelRepository repository;

    public ApplicationChannelConfigProvider(ApplicationChannelRepository repository) {
        this.repository = repository;
        this.channelIdsCache = newCache();
        this.priorityMapCache = newCache();
    }

    /**
     * 应用可见渠道集合（缓存）
     *
     * @param applicationId 应用 ID（null → 空集，不缓存）
     * @return 应用可见的渠道 ID 集合
     */
    public Set<Long> findChannelIdsByApplicationId(Long applicationId) {
        if (applicationId == null) {
            return Set.of();
        }
        return channelIdsCache.get(applicationId,
                id -> repository.findChannelIdsByApplicationId(id));
    }

    /**
     * 应用渠道优先级映射（缓存；null 优先级剔除，PriorityRouter 回退默认值）
     *
     * @param applicationId 应用 ID（null → 空映射，不缓存）
     * @return 渠道 ID → 优先级 映射
     */
    public Map<Long, Integer> findPriorityMapByApplicationId(Long applicationId) {
        if (applicationId == null) {
            return Map.of();
        }
        return priorityMapCache.get(applicationId, id -> {
            Map<Long, Integer> map = new LinkedHashMap<>();
            for (ApplicationChannel channel : repository.findByApplicationId(id)) {
                if (channel.getPriority() != null) {
                    map.put(channel.getChannelId(), channel.getPriority());
                }
            }
            return map;
        });
    }

    /**
     * 显式失效（配置写操作后调用——变更即时生效）
     *
     * @param applicationId 应用 ID
     */
    public void evict(Long applicationId) {
        channelIdsCache.invalidate(applicationId);
        priorityMapCache.invalidate(applicationId);
    }

    private static <K, V> Cache<K, V> newCache() {
        return Caffeine.newBuilder()
                .maximumSize(500)
                .expireAfterWrite(60, TimeUnit.SECONDS)
                .recordStats()
                .build();
    }
}
