package com.codingas.gateway.application.listener;

import com.codingas.gateway.core.domain.event.TokenUsedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Token 使用事件监听器
 *
 * <p>异步处理 Token 使用事件，用于：</p>
 * <ul>
 *   <li>Token 使用量统计</li>
 *   <li>预算控制检查</li>
 *   <li>成本分析</li>
 *   <li>审计日志记录</li>
 * </ul>
 *
 * <p>设计原则：
 * <ul>
 *   <li>事件监听是异步的，不影响主请求流程</li>
 *   <li>每个监听器专注于单一职责</li>
 *   <li>监听器不返回结果，不阻塞事件发布</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TokenUsageEventListener {

    /**
     * 处理 Token 使用事件
     */
    @Async
    @EventListener
    public void handleTokenUsedEvent(TokenUsedEvent event) {
        log.info("Token usage: model={}, userId={}, prompt={}, completion={}, total={}",
                event.model(),
                event.userId(),
                event.promptTokens(),
                event.completionTokens(),
                event.totalTokens());

        // TODO: 实现以下功能：
        // 1. 更新 TokenLimit 记录的使用量
        // 2. 检查预算是否超出
        // 3. 发送预算警告通知
        // 4. 记录审计日志
    }
}
