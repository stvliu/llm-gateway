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
package com.codingas.gateway.application.resilience.event;

import com.codingas.gateway.common.event.FailoverOccurredEvent;
import com.codingas.gateway.domain.resilience.entity.FailoverEvent;
import com.codingas.gateway.domain.resilience.gateway.FailoverEventGateway;
import com.codingas.gateway.common.enums.FailoverDecision;
import com.codingas.gateway.common.enums.ProviderErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

/**
 * FailoverEventListener 事件发布集成测试
 *
 * <p>守护 C2 修复核心点：调用链 {@code ChatDispatchServiceImpl.dispatch} 无 {@code @Transactional}，
 * {@code FailoverOccurredEvent} 由 {@code ApplicationEventPublisher} 在无事务上下文下发布。
 * 原实现 {@code @TransactionalEventListener(AFTER_COMMIT)} 在无事务时静默丢弃事件（fallbackExecution
 * 默认 false），监听器永不执行，转移事件全部丢失。修复后改用 {@code @EventListener}（非事务监听），
 * 无事务上下文下事件仍被处理。</p>
 *
 * <p>本测试在无事务的方法中发布事件，断言 {@link FailoverEventGateway#save} 被调用——
 * 用 {@code @TransactionalEventListener} 时此断言失败（事件丢失），用 {@code @EventListener} 时通过。</p>
 *
 * <p>参照 {@code FailoverEventListenerTest}（纯 Mockito 单测，直接调方法，无法覆盖事务语义差异），
 * 本测试补足"经 Spring 事件总线在无事务上下文发布"的真实路径。</p>
 */
@SpringBootTest(classes = FailoverEventListener.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE)
@DisplayName("FailoverEventListener 无事务上下文事件发布集成测试")
class FailoverEventListenerPublishTest {

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @MockBean
    private FailoverEventGateway failoverEventGateway;

    /**
     * 无事务上下文下发布转移事件，监听器仍应持久化事件（非事务监听，不依赖事务提交）
     *
     * <p>RED（修复前）：{@code @TransactionalEventListener(AFTER_COMMIT)} 无事务时事件静默丢弃，
     * save 永不被调用，timeout 断言失败。</p>
     * <p>GREEN（修复后）：{@code @EventListener} 无事务时同步触发，save 被调用。</p>
     */
    @Test
    @DisplayName("无事务上下文下发布事件仍触发监听器持久化")
    void publishEvent_withoutTransaction_listenerStillPersists() {
        FailoverOccurredEvent event = new FailoverOccurredEvent(
                "trace-no-tx-1",
                7L,
                10L, 20L,
                11L, 21L,
                ProviderErrorType.AUTHENTICATION_ERROR,
                FailoverDecision.L1,
                false,
                Instant.now()
        );

        // 故意不在 @Transactional 方法中发布，模拟 ChatDispatchServiceImpl.dispatch 无事务的真实调用链
        eventPublisher.publishEvent(event);

        // timeout 兼容潜在 @Async（当前未启用 @EnableAsync，同步执行立即完成）
        verify(failoverEventGateway, timeout(2000)).save(any(FailoverEvent.class));
    }
}
