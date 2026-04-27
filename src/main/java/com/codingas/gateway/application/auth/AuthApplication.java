package com.codingas.gateway.application.auth;

import com.codingas.gateway.domain.security.service.AuthenticationService;
import com.codingas.gateway.domain.security.service.RbacService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 认证用例应用服务
 *
 * <p>编排认证相关的领域服务，不含业务逻辑。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthApplication {

    private final AuthenticationService authenticationService;
    private final RbacService rbacService;

    /**
     * 认证 API Key
     *
     * @param apiKey API Key
     * @param clientIp 客户端 IP
     * @return 认证结果
     */
    public AuthenticationService.UserAuthResult authenticate(String apiKey, String clientIp) {
        var result = authenticationService.authenticate(apiKey);
        if (result != null) {
            log.info("API Key authenticated: userId={}, keyCode={}, ip={}",
                result.userId(), result.keyCode(), clientIp);
        } else {
            log.warn("API Key authentication failed: ip={}", clientIp);
        }
        return result;
    }

    /**
     * 检查用户权限
     *
     * @param userId 用户 ID
     * @param resource 资源
     * @param action 操作
     * @return 是否有权
     */
    public boolean checkPermission(Long userId, String resource, String action) {
        var userOpt = authenticationService.getUserById(userId);
        return userOpt.map(user -> rbacService.hasPermission(user, resource, action))
            .orElse(false);
    }
}
