package com.codingas.gateway.domain.supply.enums;

/**
 * 模型规格状态枚举
 *
 * <h3>状态转换图</h3>
 * <pre>
 * ACTIVE ⇄ DISABLED
 *    ↓
 * DELETED（终态）
 * </pre>
 */
public enum ModelSpecState {
    /** 正常运行，可接受请求 */
    ACTIVE,

    /** 已禁用，不接受请求（可恢复） */
    DISABLED,

    /** 已删除（终态） */
    DELETED;

    /**
     * 判断是否可接受请求
     */
    public boolean isAvailable() {
        return this == ACTIVE;
    }

    /**
     * 判断是否为终态
     */
    public boolean isTerminal() {
        return this == DELETED;
    }
}