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

import java.time.Instant;

/**
 * 审计日志网关接口
 *
 * <p>定义在 domain 层，由 infrastructure 层实现。</p>
 */
public interface AuditLogRepository {

    /**
     * 保存调用日志
     *
     * @param callLog 调用日志实体
     * @return 保存后的实体
     */
    CallLog saveCallLog(CallLog callLog);

    /**
     * 保存操作日志（管理操作审计）
     *
     * @param auditLog 审计日志实体
     * @return 保存后的实体
     */
    AuditLog saveAuditLog(AuditLog auditLog);

    /**
     * 分页查询操作日志
     *
     * @param query 查询条件（分页 + 筛选）
     * @return 分页结果
     */
    PageResponse<AuditLog> findAuditLogs(AuditLogQuery query);

    /**
     * 删除创建时间早于截止时间的审计日志
     *
     * <p>用于审计日志保留策略清理：定时任务与手动清理端点共用此能力。</p>
     *
     * @param cutoff 截止时间（不含），createdAt 早于该时间的日志将被删除
     * @return 删除的条数
     */
    int deleteBefore(Instant cutoff);
}
