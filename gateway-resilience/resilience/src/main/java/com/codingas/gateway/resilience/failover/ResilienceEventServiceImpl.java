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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * 转移事件查询应用服务实现
 *
 * <p>委托 {@link FailoverEventRepository} 直接返回 {@link FailoverEvent} 实体，
 * 枚举字段转字符串展示由 web 层 DTO 转换负责。</p>
 *
 * <p><b>耗尽告警默认窗口</b>：{@code findExhausted} 当 since 为 null 时由 Service 层补默认窗口
 * 最近 1 小时（设计 D12「耗尽告警」语义为近期告警），避免透传 null 返回全量历史耗尽事件
 * 导致前端总览页告警区被陈旧数据淹没。{@code findRecent} 不补默认窗口（转移事件流需支持
 * 回溯历史，since 为 null 时按 limit 截断全量倒序）。</p>
 *
 * <p>遵循 Service→Gateway 分层范式。</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ResilienceEventServiceImpl implements ResilienceEventService {

    /** 耗尽告警默认查询窗口：最近 1 小时（设计 D12） */
    private static final Duration EXHAUSTED_DEFAULT_WINDOW = Duration.ofHours(1);

    private final FailoverEventRepository failoverEventRepository;

    @Override
    public List<FailoverEvent> findRecent(Instant since, Long applicationId, int limit) {
        return failoverEventRepository.findRecent(since, applicationId, limit);
    }

    @Override
    public List<FailoverEvent> findExhausted(Instant since, int limit) {
        // since 为 null 时补默认窗口最近 1 小时（耗尽告警语义为近期告警，见类 Javadoc）
        Instant effectiveSince = since != null ? since : Instant.now().minus(EXHAUSTED_DEFAULT_WINDOW);
        return failoverEventRepository.findExhausted(effectiveSince, limit);
    }
}
