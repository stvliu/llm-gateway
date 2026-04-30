package com.codingas.gateway.application.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * 网关拦截器抽象基类
 *
 * <p>提供通用的日志记录和便捷方法。</p>
 */
@Slf4j
public abstract class AbstractGatewayInterceptor implements GatewayInterceptor {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public int order() {
        return 0; // 默认顺序，子类可覆盖
    }

    /**
     * 获取客户端真实 IP（支持代理场景）
     */
    protected String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isBlank()) {
            return xRealIp.trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * 短路处理，返回 403
     */
    protected void reject(HttpServletResponse response, String message) throws Exception {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(OBJECT_MAPPER.writeValueAsString(
                new ErrorResponse("ACCESS_DENIED", message)));
    }

    /**
     * 短路处理，返回 401
     */
    protected void unauthorized(HttpServletResponse response, String message) throws Exception {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(OBJECT_MAPPER.writeValueAsString(
                new ErrorResponse("UNAUTHORIZED", message)));
    }

    /**
     * JSON 错误响应结构
     */
    private record ErrorResponse(String code, String message) {}
}
