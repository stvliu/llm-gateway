package com.codingas.gateway.adapter.interceptor;

import com.codingas.gateway.domain.security.service.AuthenticationDomainService;
import com.codingas.gateway.domain.security.service.UserAuthResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * API Key 认证拦截器
 *
 * <p>责任链第二个拦截器，验证 API Key 并加载用户信息。</p>
 */
@Slf4j
@RequiredArgsConstructor
public class ApiKeyAuthInterceptor extends AbstractGatewayInterceptor {

    public static final String API_KEY_HEADER = "X-API-Key";
    public static final String USER_ID_ATTR = "userId";

    private final AuthenticationDomainService authenticationService;

    @Override
    public String name() {
        return "ApiKeyAuth";
    }

    @Override
    public int order() {
        return 2; // IP检查之后
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response) {
        String apiKey = request.getHeader(API_KEY_HEADER);

        if (apiKey == null || apiKey.isBlank()) {
            log.warn("Missing API Key in request to {}", request.getRequestURI());
            try {
                unauthorized(response, "Missing API Key");
            } catch (Exception e) {
                log.error("Failed to write unauthorized response", e);
            }
            return false;
        }

        UserAuthResult userInfo = authenticationService.authenticate(apiKey);
        if (userInfo == null) {
            log.warn("Invalid API Key for request to {}", request.getRequestURI());
            try {
                unauthorized(response, "Invalid API Key");
            } catch (Exception e) {
                log.error("Failed to write unauthorized response", e);
            }
            return false;
        }

        // 存储用户信息到请求属性
        request.setAttribute(USER_ID_ATTR, userInfo.userId());
        request.setAttribute("userCode", userInfo.userCode());
        request.setAttribute("apiKeyId", userInfo.apiKeyId());

        log.debug("API Key authenticated: userId={}, keyCode={}",
                userInfo.userId(), userInfo.userCode());
        return true;
    }
}
