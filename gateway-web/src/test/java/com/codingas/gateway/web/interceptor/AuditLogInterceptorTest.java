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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link AuditLogInterceptor} 管理操作审计测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuditLogInterceptor（管理操作审计）测试")
class AuditLogInterceptorTest {

    @Mock
    private BizEventPublisher eventPublisher;

    @InjectMocks
    private AuditLogInterceptor interceptor;

    private HttpServletRequest request(String method, String uri, Long userId) {
        HttpServletRequest req = mock(HttpServletRequest.class);
        // lenient：读操作/非管理路径/仅 preHandle 场景不会读取全部属性，避免 UnnecessaryStubbing
        lenient().when(req.getRequestURI()).thenReturn(uri);
        lenient().when(req.getMethod()).thenReturn(method);
        lenient().when(req.getAttribute("userId")).thenReturn(userId);
        lenient().when(req.getHeader("X-Forwarded-For")).thenReturn(null);
        lenient().when(req.getHeader("X-Real-IP")).thenReturn(null);
        lenient().when(req.getRemoteAddr()).thenReturn("127.0.0.1");
        return req;
    }

    private HttpServletResponse response(int status) {
        HttpServletResponse resp = mock(HttpServletResponse.class);
        // lenient：不发布事件的场景不读取状态
        lenient().when(resp.getStatus()).thenReturn(status);
        return resp;
    }

    @Test
    @DisplayName("管理写操作发布审计事件（userId/action/资源/IP/状态透传）")
    void afterCompletion_writeMethod_publishesAuditEvent() {
        // given
        HttpServletRequest req = request("POST", "/api/v1/channels", 5L);

        // when
        interceptor.afterCompletion(req, response(200), null, null);

        // then
        ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(eventPublisher).publish(captor.capture());
        AuditEvent event = captor.getValue();
        assertThat(event.userId()).isEqualTo(5L);
        assertThat(event.action()).isEqualTo("POST /api/v1/channels");
        assertThat(event.resource()).isEqualTo("/api/v1/channels");
        assertThat(event.clientIp()).isEqualTo("127.0.0.1");
        assertThat(event.responseStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("GET 读操作不发布审计事件")
    void afterCompletion_readMethod_doesNotPublish() {
        // given
        HttpServletRequest req = request("GET", "/api/v1/channels", 5L);

        // when
        interceptor.afterCompletion(req, response(200), null, null);

        // then
        verify(eventPublisher, never()).publish(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("非管理路径（/v1/ 数据面）不发布审计事件")
    void afterCompletion_nonManagedPath_doesNotPublish() {
        // given
        HttpServletRequest req = request("POST", "/v1/chat/completions", null);

        // when
        interceptor.afterCompletion(req, response(200), null, null);

        // then
        verify(eventPublisher, never()).publish(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("未认证主体（无 userId，如登录请求）userId 归 0 发布")
    void afterCompletion_withoutUserId_publishesWithZero() {
        // given：登录请求，TokenAuth 未注入 userId
        HttpServletRequest req = request("POST", "/api/v1/auth/login", null);

        // when：登录失败 401
        interceptor.afterCompletion(req, response(401), null, null);

        // then
        ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(eventPublisher).publish(captor.capture());
        AuditEvent event = captor.getValue();
        assertThat(event.userId()).isZero();
        assertThat(event.action()).isEqualTo("POST /api/v1/auth/login");
        assertThat(event.responseStatus()).isEqualTo(401);
    }

    @Test
    @DisplayName("preHandle 始终放行（审计不拦截请求）")
    void preHandle_alwaysPasses() {
        // given
        HttpServletRequest req = request("DELETE", "/api/v1/users/1", 1L);

        // when/then
        assertThat(interceptor.preHandle(req, response(200), null)).isTrue();
    }
}
