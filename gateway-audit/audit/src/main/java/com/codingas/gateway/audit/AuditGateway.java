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

import java.util.List;

/**
 * 审计日志网关接口
 *
 * <p>定义在 domain 层，由 infrastructure 层实现。</p>
 */
public interface AuditGateway {

    /**
     * 保存审计日志
     *
     * @param auditLog 审计日志实体
     * @return 保存后的实体
     */
    AuditLog save(AuditLog auditLog);

    /**
     * 根据用户 ID 查找审计日志
     *
     * @param userId 用户 ID
     * @return 审计日志列表
     */
    List<AuditLog> findByUserId(Long userId);

    /**
     * 保存调用日志
     *
     * @param callLog 调用日志实体
     * @return 保存后的实体
     */
    CallLog saveCallLog(CallLog callLog);
}
