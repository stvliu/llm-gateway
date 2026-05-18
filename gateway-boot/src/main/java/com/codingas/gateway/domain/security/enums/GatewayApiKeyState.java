package com.codingas.gateway.domain.security.enums;

/**
 * Gateway API Key 状态枚举
 *
 * <p>旧架构枚举，新架构使用 UserApiKeyState。</p>
 *
 * @deprecated 使用 {@link com.codingas.gateway.domain.team.enums.UserApiKeyState} 替代
 */
@Deprecated(since = "2.0", forRemoval = true)
public enum GatewayApiKeyState {
    /** 正常使用，可接受请求 */
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
