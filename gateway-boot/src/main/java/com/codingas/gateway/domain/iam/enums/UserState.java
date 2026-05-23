package com.codingas.gateway.domain.iam.enums;

/**
 * 用户状态枚举
 *
 * <p>企业内部 LLM Gateway 的用户账户生命周期状态。</p>
 *
 * <h3>状态转换图</h3>
 * <pre>
 * ACTIVE ⇄ DISABLED
 *    ↓
 * LOCKED（安全事件触发）
 * </pre>
 *
 * <h3>状态说明</h3>
 * <ul>
 *   <li>ACTIVE：账户正常，可正常使用</li>
 *   <li>DISABLED：账户禁用，无法登录（可恢复）</li>
 *   <li>LOCKED：账户锁定，登录失败次数过多触发（可恢复）</li>
 * </ul>
 */
public enum UserState {
    /** 账户正常，可正常使用 */
    ACTIVE,

    /** 账户停用，无法登录（可恢复） */
    DISABLED,

    /** 账户锁定（登录失败次数过多触发，可恢复） */
    LOCKED;

    /**
     * 判断是否可以登录
     */
    public boolean canLogin() {
        return this == ACTIVE;
    }

    /**
     * 判断是否为安全锁定状态
     */
    public boolean isLocked() {
        return this == LOCKED;
    }
}
