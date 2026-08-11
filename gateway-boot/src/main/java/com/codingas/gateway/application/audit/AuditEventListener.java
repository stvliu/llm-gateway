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
package com.codingas.gateway.application.audit;

import com.codingas.gateway.common.event.AuditEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 审计事件监听器
 *
 * <p>异步处理审计事件，用于记录安全相关操作日志。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuditEventListener {

    /**
     * 处理审计事件
     */
    @Async
    @EventListener
    public void handleAuditEvent(AuditEvent event) {
        log.info("Audit: action={}, userId={}, resource={}, clientIp={}, status={}",
                event.action(),
                event.userId(),
                event.resource(),
                event.clientIp(),
                event.responseStatus());
    }
}
