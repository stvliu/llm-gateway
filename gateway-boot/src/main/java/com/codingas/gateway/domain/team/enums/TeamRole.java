package com.codingas.gateway.domain.team.enums;

/**
 * 团队角色枚举
 */
public enum TeamRole {

    OWNER("owner"),
    ADMIN("admin"),
    MEMBER("member");

    private final String code;

    TeamRole(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static TeamRole fromCode(String code) {
        for (TeamRole role : values()) {
            if (role.code.equals(code)) {
                return role;
            }
        }
        throw new IllegalArgumentException("Unknown team role: " + code);
    }
}
