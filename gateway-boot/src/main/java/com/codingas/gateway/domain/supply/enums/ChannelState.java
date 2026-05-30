package com.codingas.gateway.domain.supply.enums;

import lombok.Getter;

/**
 * 渠道状态枚举
 *
 * <h3>状态转换图</h3>
 * <pre>
 * ACTIVE ⇄ INACTIVE
 * </pre>
 */
@Getter
public enum ChannelState {
    /** 活跃状态，可正常使用 */
    ACTIVE("active"),

    /** 已停用，暂停服务（可恢复） */
    INACTIVE("inactive");

    private final String code;

    ChannelState(String code) {
        this.code = code;
    }

    public static ChannelState fromCode(String code) {
        for (ChannelState state : values()) {
            if (state.code.equals(code)) {
                return state;
            }
        }
        throw new IllegalArgumentException("Unknown channel state: " + code);
    }

    /**
     * 判断是否可接受请求
     */
    public boolean isAvailable() {
        return this == ACTIVE;
    }
}