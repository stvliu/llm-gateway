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
package com.codingas.gateway.infrastructure.resilience;

import org.springframework.stereotype.Component;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 端点统计注册表 — 按 endpointId 索引管理 EndpointMetrics
 */
@Component
public class EndpointMetricsRegistry {

    private final ConcurrentMap<Long, EndpointMetrics> metricsMap = new ConcurrentHashMap<>();

    /**
     * 获取或创建端点的统计实例
     */
    public EndpointMetrics get(Long endpointId) {
        return metricsMap.computeIfAbsent(endpointId, k -> new EndpointMetrics());
    }

    /**
     * 获取所有统计实例
     */
    public ConcurrentMap<Long, EndpointMetrics> getAll() {
        return metricsMap;
    }

    /**
     * 移除指定端点的统计
     */
    public void remove(Long endpointId) {
        metricsMap.remove(endpointId);
    }
}
