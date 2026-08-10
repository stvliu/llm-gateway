/*
 * Copyright (c) 2025 codingas.com
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE file for details.
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
