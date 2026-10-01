package com.codingas.gateway.iam.auth;

import com.codingas.gateway.iam.application.ApplicationChannelRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.AntPathMatcher;

import java.util.List;
import java.util.Set;

/**
 * 统一授权门面
 *
 * <p>以「资源-动作-范围」统一模型承载两面授权判定：</p>
 * <ul>
 *   <li><b>控制面</b>（{@link #checkControl}）：管理 API 的功能级授权。
 *       规则代码化于 {@link #CONTROL_RULES}（与前端 {@link RolePermissions} 权限码语义对齐），
 *       scope 语义：PUBLIC 无需登录 / LOGIN_ONLY 登录即可 / USER 需 USER 或 ADMIN 角色 /
 *       未匹配规则默认拒绝（仅 ADMIN 放行）；</li>
 *   <li><b>数据面</b>（{@link #permittedChannelIds}）：应用-渠道对象级授权，
 *       委托 {@link ApplicationChannelRepository}，D9 语义保留（无角色特权旁路，
 *       applicationId 为 null 返回空集）。</li>
 * </ul>
 */
@Service
public class AuthorizationService {

    /** scope：无需登录（认证端点本身） */
    public static final String SCOPE_PUBLIC = "PUBLIC";
    /** scope：登录即可，不限角色 */
    public static final String SCOPE_LOGIN_ONLY = "LOGIN_ONLY";
    /** scope：USER 或 ADMIN 角色 */
    public static final String SCOPE_USER = "USER";

    private static final AntPathMatcher MATCHER = new AntPathMatcher();

    /**
     * 控制面授权规则表（代码化权限表，阶段 1 不落 DB）
     *
     * <p>与历史 PermissionInterceptor 四个静态规则列表逐条对应，语义零变化。
     * resource/action 与 {@link RolePermissions} USER 权限码对齐
     * （dashboard、model:read、quickstart:access、key:read、key:write、application:read）。</p>
     */
    static final List<ControlPermissionRule> CONTROL_RULES = List.of(
            // PUBLIC：无需登录（login；logout 幂等，与 GatewayAuthenticatorInterceptor 公开路径语义一致）
            new ControlPermissionRule("auth", "login", SCOPE_PUBLIC, "POST", "/api/v1/auth/login"),
            new ControlPermissionRule("auth", "logout", SCOPE_PUBLIC, "POST", "/api/v1/auth/logout"),
            // LOGIN_ONLY：登录即可（个人认证与只读能力）
            new ControlPermissionRule("me", "read", SCOPE_LOGIN_ONLY, "GET", "/api/v1/auth/me"),
            new ControlPermissionRule("me", "read", SCOPE_LOGIN_ONLY, "PATCH", "/api/v1/auth/me/password"),
            new ControlPermissionRule("me", "read", SCOPE_LOGIN_ONLY, "GET", "/api/v1/me/**"),
            new ControlPermissionRule("protocol", "read", SCOPE_LOGIN_ONLY, "GET", "/api/v1/protocols"),
            // USER 白名单：USER 或 ADMIN
            new ControlPermissionRule("model", "read", SCOPE_USER, "GET", "/api/v1/models/**"),
            new ControlPermissionRule("application", "read", SCOPE_USER, "GET", "/api/v1/applications/**"),
            new ControlPermissionRule("experience", "execute", SCOPE_USER, "POST", "/api/v1/experience/**"),
            new ControlPermissionRule("experience", "read", SCOPE_USER, "GET", "/api/v1/experience/**"),
            new ControlPermissionRule("apikey", "read", SCOPE_USER, "GET", "/api/v1/user-api-keys/*"),
            new ControlPermissionRule("apikey", "read", SCOPE_USER, "GET", "/api/v1/user-api-keys/*/*"),
            new ControlPermissionRule("apikey", "write", SCOPE_USER, "POST", "/api/v1/user-api-keys"),
            new ControlPermissionRule("apikey", "write", SCOPE_USER, "PUT", "/api/v1/user-api-keys/*"),
            new ControlPermissionRule("apikey", "write", SCOPE_USER, "DELETE", "/api/v1/user-api-keys/*")
    );

    private final ApplicationChannelRepository applicationChannelRepository;

    public AuthorizationService(ApplicationChannelRepository applicationChannelRepository) {
        this.applicationChannelRepository = applicationChannelRepository;
    }

    /**
     * 控制面授权判定（管理 API）
     *
     * <p>按方法 + 路径匹配规则表，按 scope 判定；未匹配规则默认拒绝（仅 ADMIN 放行）。
     * 调用方负责仅在管理路径（/api/v1/）下调用。</p>
     *
     * @param identity 认证身份（可为 null，表示未认证；PUBLIC 规则仍放行）
     * @param method   HTTP 方法
     * @param path     请求路径
     * @return true 允许；false 拒绝（调用方响应 403）
     */
    public boolean checkControl(Identity identity, String method, String path) {
        for (ControlPermissionRule rule : CONTROL_RULES) {
            if (!rule.method().equals(method)) {
                continue;
            }
            if (!MATCHER.match(rule.pathPattern(), path)) {
                continue;
            }
            return switch (rule.scope()) {
                case SCOPE_PUBLIC -> true;
                case SCOPE_LOGIN_ONLY -> identity != null;
                case SCOPE_USER -> identity != null
                        && (RolePermissions.ROLE_USER.equals(identity.role())
                        || RolePermissions.ROLE_ADMIN.equals(identity.role()));
                default -> false;
            };
        }
        // 未匹配规则的管理路径：默认拒绝，仅 ADMIN 放行
        return identity != null && RolePermissions.ROLE_ADMIN.equals(identity.role());
    }

    /**
     * 数据面授权：应用角色可见渠道集合
     *
     * <p>统一 RBAC 语义下，applicationId 即「APPLICATION 类型角色」ID——本方法
     * 按角色解析可见渠道权限（委托 {@link ApplicationChannelRepository}）。
     * D9 语义保留：无用户角色特权旁路；applicationId 为 null（无角色）返回空集。</p>
     *
     * @param applicationId 应用 ID（数据面角色锚点）
     * @return 该角色（应用）可见的渠道 ID 集合
     */
    public Set<Long> permittedChannelIds(Long applicationId) {
        if (applicationId == null) {
            return Set.of();
        }
        return applicationChannelRepository.findChannelIdsByApplicationId(applicationId);
    }
}
