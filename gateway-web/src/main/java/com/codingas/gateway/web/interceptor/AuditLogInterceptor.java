/*
 * Copyright © 2025-2026 codingas.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.codingas.gateway.web.interceptor;

import com.codingas.gateway.common.event.AuditEvent;
import com.codingas.gateway.common.event.BizEventPublisher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Instant;
import java.util.Set;

/**
 * 管理操作审计拦截器
 *
 * <p>独立的 Spring {@link HandlerInterceptor}（非安全责任链成员），在请求完成时
 * （{@link #afterCompletion}，可拿到最终响应状态）对管理 API 写操作发布
 * {@link AuditEvent}，由 audit 域监听器落库。</p>
 *
 * <p>审计范围：{@code /api/v1/**} 的 POST/PUT/PATCH/DELETE。
 * 操作人取 {@link TokenAuthInterceptor} 注入的 {@code userId} request attribute；
 * 未认证主体（如登录请求，userId 为空）以 0 记录，规避 audit_logs.user_id NOT NULL 约束。
 * 登录成功/失败随响应状态自动覆盖（login 为公开路径，必然走到 afterCompletion）。
 * 审计发布失败仅记录日志，不影响主流程。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuditLogInterceptor implements HandlerInterceptor {

    /** 管理 API 路径前缀（需审计的写操作） */
    private static final String MANAGED_PREFIX = "/api/v1/";

    /** 写操作方法（读操作不审计） */
    private static final Set<String> WRITE_METHODS = Set.of("POST", "PUT", "PATCH", "DELETE");

    private final BizEventPublisher eventPublisher;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 审计不拦截请求，始终放行
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        try {
            String uri = request.getRequestURI();
            String method = request.getMethod();
            if (!uri.startsWith(MANAGED_PREFIX) || !WRITE_METHODS.contains(method)) {
                return;
            }

            Long userId = (Long) request.getAttribute("userId");
            AuditEvent event = AuditEvent.builder()
                    .userId(userId != null ? userId : 0L)
                    .action(method + " " + uri)
                    .resource(uri)
                    .clientIp(getClientIp(request))
                    .responseStatus(response.getStatus())
                    .occurredOn(Instant.now())
                    .build();
            eventPublisher.publish(event);
        } catch (Exception e) {
            log.error("发布审计事件失败: {} {}", request.getMethod(), request.getRequestURI(), e);
        }
    }

    /**
     * 获取客户端真实 IP（支持代理场景）
     */
    private String getClientIp(HttpServletRequest request) {
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
}
