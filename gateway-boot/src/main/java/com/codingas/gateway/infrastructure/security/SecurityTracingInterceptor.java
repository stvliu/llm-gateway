package com.codingas.gateway.infrastructure.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.UUID;

/**
 * 安全事件追踪拦截器
 *
 * <p>所有安全事件（认证/授权/限流/脱敏）均记录 trace_id。</p>
 * <p>支持 OpenTelemetry 集成（当配置了 OpenTelemetry SDK 时）。</p>
 */
@Slf4j
@Component
public class SecurityTracingInterceptor implements HandlerInterceptor {

    public static final String TRACE_ID_HEADER = "X-Trace-ID";
    public static final String TRACE_ATTR = "security.trace_id";
    public static final String SPAN_ATTR = "security.span";
    public static final String START_TIME_ATTR = "security.start_time";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 获取或生成 trace ID
        String traceId = request.getHeader(TRACE_ID_HEADER);
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString().replace("-", "");
        }

        // 设置到响应头
        response.setHeader(TRACE_ID_HEADER, traceId);

        // 存储到请求属性供后续使用
        request.setAttribute(TRACE_ATTR, traceId);
        request.setAttribute(START_TIME_ATTR, System.currentTimeMillis());

        log.debug("Security trace started: traceId={}, uri={}, clientIp={}",
            traceId, request.getRequestURI(), getClientIp(request));

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
            Object handler, Exception ex) {

        String traceId = (String) request.getAttribute(TRACE_ATTR);
        Long startTime = (Long) request.getAttribute(START_TIME_ATTR);

        if (startTime != null) {
            long duration = System.currentTimeMillis() - startTime;
            int status = response.getStatus();

            log.debug("Security trace completed: traceId={}, status={}, duration={}ms",
                traceId, status, duration);

            // 记录错误
            if (ex != null) {
                log.warn("Security request failed: traceId={}, error={}", traceId, ex.getMessage());
            }
        }
    }

    /**
     * 获取客户端 IP
     */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isBlank()) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }

    /**
     * 获取当前请求的 trace ID
     */
    public static String getTraceId(HttpServletRequest request) {
        return (String) request.getAttribute(TRACE_ATTR);
    }
}
