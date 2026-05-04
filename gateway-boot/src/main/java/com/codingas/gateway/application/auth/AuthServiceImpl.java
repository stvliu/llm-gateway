package com.codingas.gateway.application.auth;

import com.codingas.gateway.domain.security.service.AuthenticationDomainService;
import com.codingas.gateway.domain.security.service.UserAuthResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 认证应用服务实现
 *
 * <p>编排认证相关的领域服务，不含业务逻辑。</p>
 * <p>简化权限模型：通过 User.role 字段判断管理员/普通用户。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationDomainService authenticationService;

    /**
     * 认证 API Key
     *
     * @param apiKey API Key
     * @param clientIp 客户端 IP
     * @return 认证结果
     */
    @Override
    public UserAuthResult authenticate(String apiKey, String clientIp) {
        var result = authenticationService.authenticate(apiKey);
        if (result != null) {
            log.info("API Key authenticated: userId={}, keyCode={}, ip={}",
                result.userId(), result.apiKeyCode(), clientIp);
        } else {
            log.warn("API Key authentication failed: ip={}", clientIp);
        }
        return result;
    }

    /**
     * 检查用户权限
     *
     * <p>简化权限模型：管理员(ADMIN)拥有所有权限，普通用户(USER)仅拥有个人权限。</p>
     *
     * @param userId 用户 ID
     * @param resource 资源
     * @param action 操作
     * @return 是否有权
     */
    @Override
    public boolean checkPermission(Long userId, String resource, String action) {
        var userOpt = authenticationService.getUserById(userId);
        return userOpt.map(user -> {
            // 管理员拥有所有权限
            if (user.isAdmin()) {
                return true;
            }
            // 普通用户仅能访问自己的资源
            // TODO: 根据具体业务场景实现细粒度权限控制
            return false;
        }).orElse(false);
    }
}
