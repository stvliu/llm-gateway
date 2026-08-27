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
package com.codingas.gateway.audit.event;

import com.codingas.gateway.audit.AuditLog;
import com.codingas.gateway.audit.AuditLogRepository;
import com.codingas.gateway.common.event.AuditEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 审计事件监听器
 *
 * <p>将管理操作审计落库（audit_logs 表）。当前为同步落库（项目未启用 @EnableAsync，
 * {@code @Async} 会静默失效）：由 AuditLogInterceptor 在请求完成阶段（响应已提交）触发，
 * 单次 INSERT 开销对管理台写操作频率可接受，且数据即时一致、审计不丢。
 * 落库失败仅记录日志，不影响主业务流程。若需异步化，需在 boot 启用 @EnableAsync
 * 并配置有界线程池。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuditEventListener {

    private final AuditLogRepository auditLogRepository;

    /**
     * 处理审计事件并落库
     */
    @EventListener
    public void handleAuditEvent(AuditEvent event) {
        log.info("Audit: action={}, userId={}, resource={}, clientIp={}, status={}",
                event.action(),
                event.userId(),
                event.resource(),
                event.clientIp(),
                event.responseStatus());
        try {
            AuditLog auditLog = new AuditLog();
            // user_id 列为 NOT NULL，未认证主体（如登录请求）以 0 表示
            auditLog.setUserId(event.userId() != null ? event.userId() : 0L);
            auditLog.setAction(event.action());
            auditLog.setResource(event.resource());
            auditLog.setResult(event.responseStatus() != null && event.responseStatus() < 400
                    ? "SUCCESS" : "FAILURE");
            auditLog.setIpAddress(event.clientIp());
            auditLogRepository.saveAuditLog(auditLog);
        } catch (Exception e) {
            log.error("保存审计日志失败: action={}, userId={}", event.action(), event.userId(), e);
        }
    }
}
