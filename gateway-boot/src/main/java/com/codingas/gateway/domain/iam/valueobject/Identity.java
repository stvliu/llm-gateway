/*
 * Copyright © 2025-2026 codingas.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.codingas.gateway.domain.iam.valueobject;

/**
 * 认证后的身份上下文
 *
 * <p>携带用户 ID、角色、凭证 ID 以及应用 ID（数据面权限锚点）。
 * 数据面权限路由（{@code PermissionRouter}）依据 {@code applicationId}
 * 判定可见渠道集合；{@code applicationId} 为 null 时权限路由返回空集。</p>
 *
 * @param userId        用户 ID
 * @param role          用户角色
 * @param credentialId  凭证 ID（UserApiKey ID）
 * @param applicationId 应用 ID（权限锚点；为 null 时权限路由返回空集）
 */
public record Identity(
        Long userId,
        String role,
        Long credentialId,
        Long applicationId
) {
    /**
     * 创建身份上下文
     *
     * @param userId        用户 ID
     * @param role          用户角色
     * @param credentialId  凭证 ID（UserApiKey ID）
     * @param applicationId 应用 ID（权限锚点；为 null 时权限路由返回空集）
     * @return 身份上下文
     */
    public static Identity of(Long userId, String role, Long credentialId, Long applicationId) {
        return new Identity(userId, role, credentialId, applicationId);
    }
}
