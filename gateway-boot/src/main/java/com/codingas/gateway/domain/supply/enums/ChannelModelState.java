package com.codingas.gateway.domain.supply.enums;

/**
 * 渠道模型状态枚举
 *
 * <h3>状态转换图</h3>
 * <pre>
 * ACTIVE ⇄ DISABLED
 *    ↓
 * DELETED（终态）
 * </pre>
 */
public enum ChannelModelState {
    /** 正常运行 */
    ACTIVE,

    /** 已禁用（可恢复） */
    DISABLED,

    /** 已删除（终态） */
    DELETED;

    /**
     * 判断是否可用
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