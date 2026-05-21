package com.codingas.gateway.domain.security.service;

/**
 * 用户认证结果
 * <p>
 * 认证成功后携带用户信息和 API Key 信息。
 * productId 已移除——一个 Key 可关联多个产品，路由时按 model name 匹配。
 */
public record UserAuthResult(
        Long userId,
        String role,
        Long userApiKeyId,
        Long teamId
) {

    /** 创建新架构认证结果 */
    public static UserAuthResult newArch(Long userId, String role,
                                         Long userApiKeyId, Long teamId) {
        return new UserAuthResult(userId, role, userApiKeyId, teamId);
    }
}
