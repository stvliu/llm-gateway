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
package com.codingas.gateway.iam.application;

/**
 * 应用状态枚举
 *
 * <p>应用聚合根的生命周期状态，控制是否参与流量路由。</p>
 *
 * <ul>
 *   <li>ACTIVE — 启用，可路由</li>
 *   <li>INACTIVE — 停用，不可路由</li>
 * </ul>
 */
public enum ApplicationState {
    ACTIVE,
    INACTIVE;

    /**
     * 是否可参与路由
     *
     * <p>仅 ACTIVE 状态可路由；INACTIVE 状态不参与流量分配。</p>
     *
     * @return 当前状态为 ACTIVE 时返回 true，否则 false
     */
    public boolean isRoutable() {
        return this == ACTIVE;
    }
}
