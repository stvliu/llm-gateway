package com.codingas.gateway.domain.iam.service;

/**
 * 认证后的身份上下文
 *
 * @param userId       用户 ID
 * @param role         用户角色
 * @param credentialId 凭证 ID（UserApiKey ID）
 */
public record Identity(
        Long userId,
        String role,
        Long credentialId
) {
    /** 创建身份 */
    public static Identity of(Long userId, String role, Long credentialId) {
        return new Identity(userId, role, credentialId);
    }
}
