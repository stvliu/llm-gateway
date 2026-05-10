package com.codingas.gateway.domain.model.enums;

/**
 * 模型状态枚举
 *
 * <p>模型状态相对简单，主要关注启用/禁用。</p>
 *
 * <h3>状态转换图</h3>
 * <pre>
 * ACTIVE ⇄ DISABLED
 *    ↓
 * DELETED（终态）
 * </pre>
 */
public enum ModelState {
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
