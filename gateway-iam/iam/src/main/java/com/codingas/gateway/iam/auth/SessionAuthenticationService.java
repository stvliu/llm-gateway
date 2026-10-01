package com.codingas.gateway.iam.auth;

import cn.dev33.satoken.stp.StpUtil;
import com.codingas.gateway.iam.user.User;
import com.codingas.gateway.iam.user.UserRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * 会话认证服务（管理面）
 *
 * <p>基于 Sa-Token 会话状态构建统一身份 {@link Identity}：
 * 已登录 → 查 users.role 填充角色（角色级授权的事实源）；未登录 → 返回空。
 * 管理面身份的 credentialId / applicationId 恒为 null（数据面权限锚点仅属 API Key 身份）。</p>
 */
@Service
public class SessionAuthenticationService {

    private final UserRepository userRepository;

    public SessionAuthenticationService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * 认证当前会话并构建统一身份
     *
     * @return 已登录时的身份；未登录返回空
     */
    public Optional<Identity> authenticateSession() {
        if (!StpUtil.isLogin()) {
            return Optional.empty();
        }
        Long userId = StpUtil.getLoginIdAsLong();
        String role = userRepository.findById(userId)
                .map(User::getRole)
                .orElse(null);
        return Optional.of(Identity.of(userId, role, null, null));
    }
}
