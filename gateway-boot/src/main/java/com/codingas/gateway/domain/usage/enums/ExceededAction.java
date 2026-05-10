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
