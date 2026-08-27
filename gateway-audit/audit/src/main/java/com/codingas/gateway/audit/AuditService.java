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

/**
 * 审计日志管理服务（查询门面）
 *
 * <p>提供审计日志分页查询能力，供管理台查询管理操作记录。</p>
 */
public interface AuditService {

    /**
     * 分页查询审计日志
     *
     * @param query 查询条件（分页 + 筛选）
     * @return 分页结果
     */
    PageResponse<AuditLog> query(AuditLogQuery query);
}
