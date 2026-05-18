package com.codingas.gateway.domain.security.service;

/**
 * 用户认证结果
 *
 * <p>包含认证成功后的用户信息和关联的 API Key 信息。</p>
 * <p>支持新旧两种架构：新架构使用 productId/userApiKeyId/teamId，旧架构使用 providerId。</p>
 */
public record UserAuthResult(
    Long userId,
    String role,
    Long apiKeyId,
    Long productId,
    Long userApiKeyId,
    Long teamId,
    boolean newArchitecture
) {

    /**
     * 创建旧架构认证结果
     */
    public static UserAuthResult legacy(Long userId, String role, Long apiKeyId) {
        return new UserAuthResult(userId, role, apiKeyId, null, null, null, false);
    }

    /**
     * 创建新架构认证结果
     */
    public static UserAuthResult newArch(Long userId, String role, Long apiKeyId,
                                          Long productId, Long userApiKeyId, Long teamId) {
        return new UserAuthResult(userId, role, apiKeyId, productId, userApiKeyId, teamId, true);
    }
}
