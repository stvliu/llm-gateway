package com.codingas.gateway.domain.supply.enums;

/**
 * 渠道模型状态枚举
 *
 * <h3>状态转换图</h3>
 * <pre>
 * ACTIVE ⇄ INACTIVE
 * </pre>
 */
public enum ChannelModelState {
    /** 正常运行 */
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