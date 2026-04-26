package com.codingas.gateway.application.listener;

import com.codingas.gateway.core.domain.event.AuditEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 审计事件监听器
 *
 * <p>异步处理审计事件，用于记录安全相关操作日志。</p>
 *
 * <p>设计原则：
 * <ul>
 *   <li>事件监听是异步的，不影响主请求流程</li>
 *   <li>审计日志需要持久化，不应丢失</li>
 *   <li>监听器专注于审计，不处理业务逻辑</li>
 * </ul>
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

        // TODO: 实现审计日志持久化
        // - 调用 AuditGateway 保存审计日志
        // - 处理审计日志保留策略
    }
}
