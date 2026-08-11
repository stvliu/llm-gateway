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
package com.codingas.gateway.common.event;

/**
 * 领域事件发布器
 *
 * <p>通用接口，支持本地和远程两种实现。</p>
 */
@FunctionalInterface
public interface DomainEventPublisher {

    /**
     * 发布领域事件
     *
     * @param event 领域事件
     * @param <T> 事件类型
     */
    <T extends DomainEvent> void publish(T event);
}
