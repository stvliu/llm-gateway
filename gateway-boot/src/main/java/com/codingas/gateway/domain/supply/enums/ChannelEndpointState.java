package com.codingas.gateway.domain.supply.enums;

/**
 * 渠道端点状态枚举
 */
public enum ChannelEndpointState {

    ACTIVE("active"),
    DISABLED("disabled");

    private final String code;

    ChannelEndpointState(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static ChannelEndpointState fromCode(String code) {
        for (ChannelEndpointState state : values()) {
            if (state.code.equalsIgnoreCase(code)) {
                return state;
            }
        }
        throw new IllegalArgumentException("Unknown channel endpoint state: " + code);
    }

    /**
     * 判断端点是否可用
     */
    public boolean isAvailable() {
        return this == ACTIVE;
    }
}
