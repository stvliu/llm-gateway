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
package com.codingas.gateway.web.api;

import com.codingas.gateway.stats.StatsManager;
import com.codingas.gateway.web.api.dto.StatsModelUsageResponse;
import com.codingas.gateway.web.api.dto.StatsResponse;
import com.codingas.gateway.web.api.dto.StatsTrendResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 系统统计控制器
 */
@RestController
@RequestMapping("/api/v1/stats")
@RequiredArgsConstructor
public class StatsController {

    private final StatsManager statsManager;

    /**
     * 获取系统统计数据
     */
    @GetMapping
    public StatsResponse getStats() {
        return StatsResponse.from(statsManager.getStats());
    }

    /**
     * 获取最近 N 天调用趋势（按天，从旧到新）
     *
     * @param days 天数，默认 7
     */
    @GetMapping("/trend")
    public List<StatsTrendResponse> trend(@RequestParam(defaultValue = "7") int days) {
        return statsManager.getTrend(days).stream()
                .map(StatsTrendResponse::from)
                .toList();
    }

    /**
     * 获取模型调用量分布（Top N）
     *
     * @param limit 条数上限，默认 5
     */
    @GetMapping("/model-usage")
    public List<StatsModelUsageResponse> modelUsage(@RequestParam(defaultValue = "5") int limit) {
        return statsManager.getModelUsage(limit).stream()
                .map(StatsModelUsageResponse::from)
                .toList();
    }
}
