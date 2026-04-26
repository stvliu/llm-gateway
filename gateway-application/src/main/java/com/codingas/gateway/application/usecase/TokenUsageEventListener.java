package com.codingas.gateway.application.usecase;

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
 *   <li>审计日志记录</li>
 * </ul>
 *
 * <p>当前为简化实现，后续迁移到 gateway-analytics 模块。</p>
 */
@Slf4j
@Component
public class TokenUsageEventListener {

    @Async
    @EventListener
    public void handleTokenUsedEvent(LLMChatUseCase.TokenUsedEvent event) {
        log.info("Token usage event: model={}, prompt={}, completion={}",
                event.model(), event.promptTokens(), event.completionTokens());

        // TODO: 后续实现：
        // 1. 调用 TokenTrackingService 更新使用量
        // 2. 检查预算限制
        // 3. 记录审计日志
    }
}
