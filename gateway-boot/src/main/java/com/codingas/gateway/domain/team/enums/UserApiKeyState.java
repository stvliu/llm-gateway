package com.codingas.gateway.domain.team.enums;

/**
 * 用户 API Key 状态枚举
 */
public enum UserApiKeyState {

    ACTIVE("active"),
    INACTIVE("inactive"),
    REVOKED("revoked");

    private final String code;

    UserApiKeyState(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public boolean isAvailable() {
        return this == ACTIVE;
    }

    public static UserApiKeyState fromCode(String code) {
        for (UserApiKeyState state : values()) {
            if (state.code.equals(code)) {
                return state;
            }
        }
        throw new IllegalArgumentException("Unknown user api key state: " + code);
    }
}
