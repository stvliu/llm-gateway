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
package com.codingas.gateway.application.init;

import com.codingas.gateway.infrastructure.config.GatewayProperties;

/**
 * 数据加载器接口
 *
 * <p>所有初始化加载器实现此接口，由 {@link DataInitializer} 按 {@link #getPhase()} 排序后依次驱动。</p>
 */
public interface DataLoader {

    /**
     * 当前加载器所属阶段（决定执行顺序）
     */
    InitPhase getPhase();

    /**
     * 是否启用。默认始终启用，子类可重写以根据配置控制开关。
     */
    default boolean isEnabled(GatewayProperties properties) {
        return true;
    }

    /**
     * 执行加载逻辑
     *
     * @param context 阶段间上下文，用于读取上游数据和写入下游数据
     */
    void load(DataLoadContext context);
}
