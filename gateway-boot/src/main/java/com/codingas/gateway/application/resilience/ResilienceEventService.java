/*
 * Copyright (c) 2025 codingas.com
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE file for details.
 */
package com.codingas.gateway.application.resilience;

import com.codingas.gateway.application.resilience.dto.FailoverEventResponse;

import java.time.Instant;
import java.util.List;

/**
 * 转移事件查询应用服务接口
 *
 * <p>提供容灾可观测性查询能力（读侧重）：转移事件流查询（分页 + since/applicationId 过滤）
 * 与耗尽告警查询。委托 {@link com.codingas.gateway.domain.resilience.gateway.FailoverEventGateway}。</p>
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
