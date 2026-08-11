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

/**
 * 数据初始化阶段
 *
 * <p>定义加载器的执行顺序。{@link #getOrder()} 值越小越先执行。</p>
 */
public enum InitPhase {

    /** 内建用户（admin），无条件执行 */
    BUILTIN_USER(10),
    /** 内建厂商数据，无条件执行 */
    BUILTIN_VENDOR(20),
    /** 示例数据（受 demo-data-enabled 控制） */
    SAMPLE_DATA(30);

    private final int order;

    InitPhase(int order) {
        this.order = order;
    }

    public int getOrder() {
        return order;
    }
}
