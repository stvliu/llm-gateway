package com.codingas.gateway.domain.security.service;

/**
 * 用户认证结果
 *
 * <p>包含认证成功后的用户信息和关联的 API Key 信息。</p>
 *
 * @param userId 用户ID
 * @param role 用户角色（ADMIN/USER）
 * @param apiKeyId API Key ID
 */
public record UserAuthResult(
    Long userId,
    String role,
    Long apiKeyId
) {
}
