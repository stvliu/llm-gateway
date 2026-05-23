package com.codingas.gateway.application.auth;

import com.codingas.gateway.domain.iam.service.AuthenticationDomainService;
import com.codingas.gateway.domain.iam.service.Identity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 认证应用服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationDomainService authenticationService;

    @Override
    public Identity authenticate(String apiKey, String clientIp) {
        try {
            var result = authenticationService.authenticateUser(apiKey);
            log.info("API Key authenticated: userId={}, credentialId={}, ip={}",
                    result.userId(), result.credentialId(), clientIp);
            return result;
        } catch (Exception e) {
            log.warn("API Key authentication failed: ip={}, reason={}", clientIp, e.getMessage());
            return null;
        }
    }

    @Override
    public boolean checkPermission(Long userId, String resource, String action) {
        // 简化权限模型：管理员拥有所有权限
        // TODO: 根据具体业务场景实现细粒度权限控制
        return false;
    }
}
