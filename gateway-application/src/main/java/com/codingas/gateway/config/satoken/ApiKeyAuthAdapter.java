package com.codingas.gateway.config.satoken;

import com.codingas.gateway.core.security.authentication.AuthenticationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * API Key 认证适配器
 *
 * <p>将 API Key 认证逻辑集成到 Sa-Token 框架。</p>
 * <p>Note: Sa-Token 依赖未添加到 gateway-application，需要后续集成。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApiKeyAuthAdapter {

    private final AuthenticationService authenticationService;

    /**
     * 验证 API Key
     *
     * @param apiKey API Key
     * @return 用户 ID，验证失败返回 null
     */
    public Long authenticate(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            return null;
        }
        try {
            var userInfo = authenticationService.authenticate(apiKey);
            if (userInfo != null) {
                // Record 使用 `userInfo.userId()` 而非 `userInfo.getUserId()`
                return userInfo.userId();
            }
        } catch (Exception e) {
            log.debug("API Key authentication failed: {}", e.getMessage());
        }
        return null;
    }
}
