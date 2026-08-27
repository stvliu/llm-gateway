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
package com.codingas.gateway.auditdata.calllog;

import com.codingas.gateway.audit.CallLog;
import com.codingas.gateway.audit.CallLogRepository;
import com.codingas.gateway.audit.DailyUsage;
import com.codingas.gateway.audit.ModelUsage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 调用日志 Gateway 实现
 *
 * <p>统计查询沿用项目既有范式（findAll + 内存过滤聚合），
 * 数据量大后可迁移 SQL/Spring Data 聚合。</p>
 */
@Component
@RequiredArgsConstructor
public class JpaCallLogRepository implements CallLogRepository {

    private final CallLogJpaRepository repository;

    @Override
    public CallLog save(CallLog callLog) {
        CallLogDo do_ = toDo(callLog);
        CallLogDo saved = repository.save(do_);
        return toEntity(saved);
    }

    @Override
    public long countSince(Instant since) {
        return repository.findAll().stream()
                .filter(c -> c.getCalledAt() != null && !c.getCalledAt().isBefore(since))
                .count();
    }

    @Override
    public long sumTokensSince(Instant since) {
        return repository.findAll().stream()
                .filter(c -> c.getCalledAt() != null && !c.getCalledAt().isBefore(since))
                .mapToLong(this::tokenOf)
                .sum();
    }

    @Override
    public List<DailyUsage> findDailyUsage(Instant start, Instant end) {
        Map<String, long[]> agg = new LinkedHashMap<>();
        repository.findAll().stream()
                .filter(c -> c.getCalledAt() != null
                        && !c.getCalledAt().isBefore(start)
                        && !c.getCalledAt().isAfter(end))
                .forEach(c -> {
                    String date = c.getCalledAt().atZone(ZoneId.systemDefault()).toLocalDate().toString();
                    agg.merge(date, new long[]{1, tokenOf(c)}, (a, b) -> new long[]{a[0] + b[0], a[1] + b[1]});
                });
        return agg.entrySet().stream()
                .map(e -> new DailyUsage(e.getKey(), e.getValue()[0], e.getValue()[1]))
                .toList();
    }

    @Override
    public List<ModelUsage> findModelUsage(int limit) {
        Map<String, Long> countByModel = new HashMap<>();
        repository.findAll().forEach(c -> {
            if (c.getModel() != null) {
                countByModel.merge(c.getModel(), 1L, Long::sum);
            }
        });
        return countByModel.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(limit)
                .map(e -> new ModelUsage(e.getKey(), e.getValue()))
                .toList();
    }

    /** 单条调用日志的 Token 消耗（输入 + 输出，null 按 0） */
    private long tokenOf(CallLogDo do_) {
        long input = do_.getInputTokens() != null ? do_.getInputTokens() : 0L;
        long output = do_.getOutputTokens() != null ? do_.getOutputTokens() : 0L;
        return input + output;
    }

    private CallLogDo toDo(CallLog entity) {
        CallLogDo do_ = new CallLogDo();
        do_.setTraceId(entity.getTraceId());
        do_.setUserId(entity.getUserId());
        do_.setModel(entity.getModel());
        do_.setChannelId(entity.getChannelId());
        do_.setChannelEndpointId(entity.getChannelEndpointId());
        do_.setInboundProtocol(entity.getInboundProtocol());
        do_.setUpstreamProtocol(entity.getUpstreamProtocol());
        do_.setDurationMs(entity.getDurationMs());
        do_.setSuccess(entity.getSuccess());
        do_.setInputTokens(entity.getInputTokens());
        do_.setOutputTokens(entity.getOutputTokens());
        do_.setErrorMessage(entity.getErrorMessage());
        do_.setCalledAt(entity.getCalledAt());
        return do_;
    }

    private CallLog toEntity(CallLogDo do_) {
        CallLog entity = new CallLog();
        entity.setId(do_.getId());
        entity.setTraceId(do_.getTraceId());
        entity.setUserId(do_.getUserId());
        entity.setModel(do_.getModel());
        entity.setChannelId(do_.getChannelId());
        entity.setChannelEndpointId(do_.getChannelEndpointId());
        entity.setInboundProtocol(do_.getInboundProtocol());
        entity.setUpstreamProtocol(do_.getUpstreamProtocol());
        entity.setDurationMs(do_.getDurationMs());
        entity.setSuccess(do_.getSuccess());
        entity.setInputTokens(do_.getInputTokens());
        entity.setOutputTokens(do_.getOutputTokens());
        entity.setErrorMessage(do_.getErrorMessage());
        entity.setCalledAt(do_.getCalledAt());
        return entity;
    }
}