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

import com.codingas.gateway.audit.CallLog;
import com.codingas.gateway.audit.AuditLogRepository;
import com.codingas.gateway.audit.CallLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * 审计日志网关实现
 *
 * <p>实现 AuditLogRepository 接口，调用日志委托 CallLogRepository 持久化。</p>
 */
@Repository
@RequiredArgsConstructor
public class JpaAuditLogRepository implements AuditLogRepository {

    private final CallLogRepository callLogRepository;

    @Override
    public CallLog saveCallLog(CallLog callLog) {
        return callLogRepository.save(callLog);
    }
}
