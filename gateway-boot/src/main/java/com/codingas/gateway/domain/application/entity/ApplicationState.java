/*
 * Copyright (c) 2025 codingas.com
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE file for details.
 */
package com.codingas.gateway.domain.application.entity;

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
