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
package com.codingas.gateway.auditdata.auditlog;

import com.codingas.gateway.audit.AuditLog;
import com.codingas.gateway.audit.AuditLogQuery;
import com.codingas.gateway.audit.CallLog;
import com.codingas.gateway.audit.AuditLogRepository;
import com.codingas.gateway.audit.CallLogRepository;
import com.codingas.gateway.common.dto.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 审计日志网关实现
 *
 * <p>实现 {@link AuditLogRepository} 接口：调用日志委托 {@link CallLogRepository} 持久化；
 * 操作日志经 {@link AuditLogJpaRepository} 落库与分页查询。</p>
 */
@Repository
@RequiredArgsConstructor
public class JpaAuditLogRepository implements AuditLogRepository {

    private final CallLogRepository callLogRepository;
    private final AuditLogJpaRepository auditLogJpaRepository;

    @Override
    public CallLog saveCallLog(CallLog callLog) {
        return callLogRepository.save(callLog);
    }

    @Override
    public AuditLog saveAuditLog(AuditLog auditLog) {
        AuditLogDo do_ = toDo(auditLog);
        AuditLogDo saved = auditLogJpaRepository.save(do_);
        return toEntity(saved);
    }

    @Override
    public PageResponse<AuditLog> findAuditLogs(AuditLogQuery query) {
        List<AuditLog> logs = auditLogJpaRepository.findAll().stream()
                .map(this::toEntity)
                .collect(Collectors.toList());

        // 筛选：操作人
        if (query.getUserId() != null) {
            logs = logs.stream()
                    .filter(l -> query.getUserId().equals(l.getUserId()))
                    .collect(Collectors.toList());
        }
        // 筛选：动作（模糊匹配）
        if (query.getAction() != null && !query.getAction().isBlank()) {
            String action = query.getAction().toLowerCase();
            logs = logs.stream()
                    .filter(l -> l.getAction() != null && l.getAction().toLowerCase().contains(action))
                    .collect(Collectors.toList());
        }
        // 筛选：结果
        if (query.getResult() != null && !query.getResult().isBlank()) {
            logs = logs.stream()
                    .filter(l -> query.getResult().equalsIgnoreCase(l.getResult()))
                    .collect(Collectors.toList());
        }
        // 筛选：时间范围
        if (query.getStartTime() != null) {
            logs = logs.stream()
                    .filter(l -> l.getCreatedAt() != null && !l.getCreatedAt().isBefore(query.getStartTime()))
                    .collect(Collectors.toList());
        }
        if (query.getEndTime() != null) {
            logs = logs.stream()
                    .filter(l -> l.getCreatedAt() != null && !l.getCreatedAt().isAfter(query.getEndTime()))
                    .collect(Collectors.toList());
        }

        // 统计
        long total = logs.size();

        // 按时间倒序（最新在前）
        logs.sort(Comparator.comparing(AuditLog::getCreatedAt,
                Comparator.nullsLast(Comparator.reverseOrder())));

        // 分页
        int offset = query.getOffset();
        int limit = query.getLimit();
        List<AuditLog> pagedLogs = logs.stream()
                .skip(offset)
                .limit(limit)
                .collect(Collectors.toList());

        return PageResponse.of(pagedLogs, query.getPage(), limit, total);
    }

    @Override
    @Transactional
    public int deleteBefore(Instant cutoff) {
        // 定时任务等无外层事务的调用方，由本方法自含事务保证 @Modifying 生效
        return auditLogJpaRepository.deleteBefore(cutoff);
    }

    private AuditLogDo toDo(AuditLog entity) {
        AuditLogDo do_ = new AuditLogDo();
        do_.setUserId(entity.getUserId());
        do_.setAction(entity.getAction());
        do_.setResource(entity.getResource());
        do_.setResult(entity.getResult());
        do_.setIpAddress(entity.getIpAddress());
        return do_;
    }

    private AuditLog toEntity(AuditLogDo do_) {
        AuditLog entity = new AuditLog();
        entity.setId(do_.getId());
        entity.setUserId(do_.getUserId());
        entity.setAction(do_.getAction());
        entity.setResource(do_.getResource());
        entity.setResult(do_.getResult());
        entity.setIpAddress(do_.getIpAddress());
        entity.setCreatedAt(do_.getCreatedAt());
        return entity;
    }
}
