package com.codingas.gateway.domain.team.enums;

/**
 * 团队状态枚举
 */
public enum TeamState {

    ACTIVE("active"),
    INACTIVE("inactive"),
    DELETED("deleted");

    private final String code;

    TeamState(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public boolean isAvailable() {
        return this == ACTIVE;
    }

    public static TeamState fromCode(String code) {
        for (TeamState state : values()) {
            if (state.code.equals(code)) {
                return state;
            }
        }
        throw new IllegalArgumentException("Unknown team state: " + code);
    }
}
