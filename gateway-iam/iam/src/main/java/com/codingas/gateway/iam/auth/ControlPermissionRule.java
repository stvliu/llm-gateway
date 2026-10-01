package com.codingas.gateway.iam.auth;

/**
 * 控制面授权规则（代码化权限表）
 *
 * <p>统一授权模型的「资源-动作-范围」元组在控制面的落地：
 * {@code resource}/{@code action} 为语义标签（与 {@link RolePermissions} 权限码对齐，
 * 供审计与未来表化使用），{@code scope} 为授权级别，{@code method}/{@code pathPattern}
 * 为判定输入（Ant 风格路径模式）。</p>
 *
 * @param resource    资源类型（如 model、apikey、me）
 * @param action      动作（如 read、write、execute、login、logout）
 * @param scope       授权级别（见 {@link AuthorizationService} 常量）
 * @param method      HTTP 方法（如 GET、POST）
 * @param pathPattern Ant 风格路径模式（如 /api/v1/models/**）
 */
public record ControlPermissionRule(
        String resource,
        String action,
        String scope,
        String method,
        String pathPattern) {
}
