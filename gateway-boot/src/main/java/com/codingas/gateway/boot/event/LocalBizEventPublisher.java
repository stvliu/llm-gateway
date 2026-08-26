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
package com.codingas.gateway.boot.event;

import com.codingas.gateway.common.event.BizEvent;
import com.codingas.gateway.common.event.BizEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * 本地事件发布器
 *
 * <p>使用 Spring ApplicationEvent，适用于单实例部署。</p>
 *
 * <p><b>profile 覆盖</b>：覆盖 local/dev/standalone/test/prod 全部 profile。单实例架构下，
 * 本地 ApplicationEvent 发布可接受（与既有 ChatDispatchManagerImpl 审计事件发布行为一致）。
 * 生产 profile（prod）也启用此 bean，避免 {@code ChannelFailoverInvoker}（构造注入
 * {@link com.codingas.gateway.common.event.BizEventPublisher}）与 {@code ChatDispatchManagerImpl}
 * 在生产启动时因无 {@link BizEventPublisher} 实现 bean 而抛
 * {@code NoSuchBeanDefinitionException}（既有架构缺陷的修复）。</p>
 */
@Component
@Profile({"local", "dev", "standalone", "test", "prod"})
@Slf4j
@RequiredArgsConstructor
public class LocalBizEventPublisher implements BizEventPublisher {

    private final ApplicationEventPublisher eventPublisher;

    @Override
    public <T extends BizEvent> void publish(T event) {
        log.debug("Publishing local event: {}", event);
        eventPublisher.publishEvent(event);
    }
}
