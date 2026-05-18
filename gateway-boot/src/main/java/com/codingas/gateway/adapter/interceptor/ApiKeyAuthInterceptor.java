package com.codingas.gateway.adapter.interceptor;

import com.codingas.gateway.domain.security.service.AuthenticationDomainService;
import com.codingas.gateway.domain.security.service.UserAuthResult;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * API Key 认证拦截器
 *
 * <p>责任链第二个拦截器，验证 API Key 并加载用户信息。</p>
 * <p>仅处理 /v1/ 开头的 API 调用路径（OpenAI/Anthropic 兼容接口）。</p>
 * <p>支持两种认证方式：</p>
 * <ul>
 *   <li>Authorization: Bearer sk-xxx（OpenAI 风格）</li>
 *   <li>X-API-Key: sk-xxx（自定义 header）</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApiKeyAuthInterceptor extends AbstractGatewayInterceptor {

    public static final String API_KEY_HEADER = "X-API-Key";
    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String USER_ID_ATTR = "userId";
    public static final String AUTH_RESULT_ATTR = "authResult";

    /** API Key 认证的路径前缀 */
    private static final String API_KEY_PATH_PREFIX = "/v1/";

    private final AuthenticationDomainService authenticationService;

    @Override
    public String name() {
        return "ApiKeyAuth";
    }

    @Override
    public int order() {
        return 2; // Token检查之后
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response) {
        String requestURI = request.getRequestURI();

        // 非 API Key 认证路径，跳过
        if (!requestURI.startsWith(API_KEY_PATH_PREFIX)) {
            log.debug("Not API Key path, skipping: {}", requestURI);
            return true;
        }

        String apiKey = extractApiKey(request);

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

        // 存储用户信息和认证结果到请求属性
        request.setAttribute(USER_ID_ATTR, userInfo.userId());
        request.setAttribute("apiKeyId", userInfo.apiKeyId());
        request.setAttribute(AUTH_RESULT_ATTR, userInfo);

        log.debug("API Key authenticated: userId={}, apiKeyId={}, newArch={}",
                userInfo.userId(), userInfo.apiKeyId(), userInfo.newArchitecture());
        return true;
    }

    /**
     * 从请求中提取 API Key
     *
     * <p>优先从 Authorization: Bearer header 提取，其次从 X-API-Key header 提取。</p>
     */
    private String extractApiKey(HttpServletRequest request) {
        // 先尝试 Authorization: Bearer 格式
        String authHeader = request.getHeader(AUTHORIZATION_HEADER);
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7).trim();
        }

        // 再尝试 X-API-Key header
        return request.getHeader(API_KEY_HEADER);
    }
}
