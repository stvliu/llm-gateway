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
package com.codingas.gateway.resilience.failover;

import com.codingas.gateway.resilience.dto.FailoverEventResponse;

import java.time.Instant;
import java.util.List;

/**
 * 转移事件查询应用服务接口
 *
 * <p>提供容灾可观测性查询能力（读侧重）：转移事件流查询（分页 + since/applicationId 过滤）
 * 与耗尽告警查询。委托 {@link FailoverEventGateway}。</p>
 *
 * <p>设计见 design doc D12：容灾总览页 10s 轮询渲染转移事件流 + 耗尽告警。</p>
 */
public interface ResilienceEventService {

    /**
     * 查询近期转移事件（按 occurredAt 倒序）
     *
     * @param since         起始时间过滤（可空）
     * @param applicationId 应用 ID 过滤（可空）
     * @param limit         返回条数上限
     * @return 转移事件响应列表（按 occurredAt 倒序）
     */
    List<FailoverEventResponse> findRecent(Instant since, Long applicationId, int limit);

    /**
     * 查询耗尽告警事件（exhausted=true，按 occurredAt 倒序）
     *
     * @param since 起始时间过滤（可空）
     * @param limit 返回条数上限
     * @return 耗尽事件响应列表（按 occurredAt 倒序）
     */
    List<FailoverEventResponse> findExhausted(Instant since, int limit);
}
