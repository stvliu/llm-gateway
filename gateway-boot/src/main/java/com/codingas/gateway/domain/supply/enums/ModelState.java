package com.codingas.gateway.domain.supply.enums;

/**
 * 模型状态枚举
 *
 * <h3>状态转换图</h3>
 * <pre>
 * ACTIVE ⇄ INACTIVE
 * </pre>
 */
public enum ModelState {
    /** 正常运行，可接受请求 */
    ACTIVE,

    /** 已停用，不接受请求（可恢复） */
    INACTIVE;

    /**
     * 判断是否可接受请求
     */
    public boolean isAvailable() {
        return this == ACTIVE;
    }
}