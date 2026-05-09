package com.codingas.gateway.common.enums;

/**
 * 用户状态枚举
 */
public enum UserStatus {
    /** 账户启用，可正常使用  */
    ENABLED,
    /** 账户禁用，无法登录 */
    DISABLED,
    /** 账户锁定（登录失败次数过多触发） */
    LOCKED
}
