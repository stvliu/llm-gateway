/*
 * Copyright (c) 2025 codingas.com
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE file for details.
 */
package com.codingas.gateway.domain.usage.enums;

/**
 * 超限动作枚举
 */
public enum ExceededAction {
    /** 直接拒绝 */
    REJECT,
    /** 降级切换 */
    DOWNGRADE
}
