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
import com.codingas.gateway.audit.CallLog;
import com.codingas.gateway.audit.AuditLogRepository;
import com.codingas.gateway.audit.CallLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 审计日志网关实现
 *
 * <p>实现 AuditLogRepository 接口，使用 JPA 进行持久化。</p>
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class JpaAuditLogRepository implements AuditLogRepository {

    private final AuditLogJpaRepository repository;
    private final CallLogRepository callLogRepository;

    @Override
    public AuditLog save(AuditLog auditLog) {
        AuditLogDo doEntity = toDo(auditLog);
        AuditLogDo saved = repository.save(doEntity);
        return toEntity(saved);
    }

    @Override
    public List<AuditLog> findByUserId(Long userId) {
        return repository.findByUserId(userId).stream()
                .map(this::toEntity)
                .toList();
    }

    @Override
    public CallLog saveCallLog(CallLog callLog) {
        return callLogRepository.save(callLog);
    }

    private AuditLog toEntity(AuditLogDo doEntity) {
        if (doEntity == null) return null;
        AuditLog entity = new AuditLog();
        entity.setUserId(doEntity.getUserId());
        entity.setAction(doEntity.getAction());
        entity.setResource(doEntity.getResource());
        entity.setResult(doEntity.getResult());
        entity.setIpAddress(doEntity.getIpAddress());
        return entity;
    }

    private AuditLogDo toDo(AuditLog entity) {
        if (entity == null) return null;
        AuditLogDo doEntity = new AuditLogDo();
        doEntity.setUserId(entity.getUserId());
        doEntity.setAction(entity.getAction());
        doEntity.setResource(entity.getResource());
        doEntity.setResult(entity.getResult());
        doEntity.setIpAddress(entity.getIpAddress());
        return doEntity;
    }
}
