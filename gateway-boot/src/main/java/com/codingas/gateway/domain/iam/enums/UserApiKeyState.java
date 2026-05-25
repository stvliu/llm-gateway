package com.codingas.gateway.domain.iam.enums;

import lombok.Getter;

/**
 * 用户 API Key 状态枚举
 */
@Getter
public enum UserApiKeyState {

    ACTIVE("active"),
    INACTIVE("inactive"),
    REVOKED("revoked");

    private final String code;

    UserApiKeyState(String code) {
        this.code = code;
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
