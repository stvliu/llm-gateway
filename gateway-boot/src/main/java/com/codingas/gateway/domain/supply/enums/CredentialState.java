package com.codingas.gateway.domain.supply.enums;

/**
 * 凭证状态枚举
 *
 * <h3>状态转换图</h3>
 * <pre>
 * ACTIVE ⇄ INACTIVE
 * </pre>
 */
public enum CredentialState {
    /** 活跃状态 */
    ACTIVE,

    /** 已停用（可恢复） */
    INACTIVE;

    /**
     * 判断是否可用
     */
    public boolean isAvailable() {
        return this == ACTIVE;
    }
}