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
package com.codingas.gateway.audit;

import com.codingas.gateway.common.dto.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * 审计日志管理服务实现
 *
 * <p>委托 {@link AuditLogRepository} 完成分页查询与按保留策略清理。</p>
 */
@Service
@RequiredArgsConstructor
public class AuditServiceImpl implements AuditService {

    private final AuditLogRepository auditLogRepository;

    @Override
    public PageResponse<AuditLog> query(AuditLogQuery query) {
        return auditLogRepository.findAuditLogs(query);
    }

    @Override
    public int deleteBefore(Instant cutoff) {
        return auditLogRepository.deleteBefore(cutoff);
    }
}
