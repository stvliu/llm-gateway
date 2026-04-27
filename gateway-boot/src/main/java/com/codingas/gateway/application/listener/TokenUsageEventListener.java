package com.codingas.gateway.application.listener;

import com.codingas.gateway.common.event.TokenUsedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Token 使用事件监听器
 *
 * <p>异步处理 Token 使用事件，用于：
 * <ul>
 *   <li>Token 使用量统计</li>
 *   <li>预算控制检查</li>
 *   <li>成本分析</li>
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
    }
}
