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
