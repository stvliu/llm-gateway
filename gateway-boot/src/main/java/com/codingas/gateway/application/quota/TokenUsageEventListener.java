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
package com.codingas.gateway.application.quota;

import com.codingas.gateway.domain.usage.event.TokenUsedEvent;
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
