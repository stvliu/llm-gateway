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
package com.codingas.gateway.adapter.interceptor;

import com.codingas.gateway.iam.auth.AuthenticationFailedException;
import com.codingas.gateway.iam.auth.AuthenticationDomainService;
import com.codingas.gateway.iam.valueobject.Identity;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ApiKeyAuthInterceptor Tests")
class ApiKeyAuthInterceptorTest {

    @Mock
    private AuthenticationDomainService authenticationService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    private ApiKeyAuthInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new ApiKeyAuthInterceptor(authenticationService);
    }

    @Test
    @DisplayName("有效API Key通过检查 - Bearer token")
    void validApiKey_bearerToken_passes() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer sk-test123");
        when(request.getRequestURI()).thenReturn("/v1/chat/completions");
        Identity identity = Identity.of(1L, "user", 1L, null);
        when(authenticationService.authenticateUser("sk-test123")).thenReturn(identity);

        boolean result = interceptor.preHandle(request, response);

        assertThat(result).isTrue();
        verify(request).setAttribute("identity", identity);
    }

    @Test
    @DisplayName("有效API Key通过检查 - X-API-Key header")
    void validApiKey_xApiKeyHeader_passes() throws Exception {
        when(request.getHeader("Authorization")).thenReturn(null);
        when(request.getHeader("x-api-key")).thenReturn("sk-test123");
        when(request.getRequestURI()).thenReturn("/v1/chat/completions");
        Identity identity = Identity.of(1L, "user", 1L, null);
        when(authenticationService.authenticateUser("sk-test123")).thenReturn(identity);

        boolean result = interceptor.preHandle(request, response);

        assertThat(result).isTrue();
        verify(request).setAttribute("identity", identity);
    }

    @Test
    @DisplayName("缺少API Key返回401")
    void missingApiKey_returns401() throws Exception {
        when(request.getHeader("Authorization")).thenReturn(null);
        when(request.getHeader("x-api-key")).thenReturn(null);
        when(request.getRequestURI()).thenReturn("/v1/chat/completions");
        StringWriter sw = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(sw));

        boolean result = interceptor.preHandle(request, response);

        assertThat(result).isFalse();
        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    }

    @Test
    @DisplayName("无效API Key返回401")
    void invalidApiKey_returns401() throws Exception {
        when(request.getHeader("Authorization")).thenReturn(null);
        when(request.getHeader("x-api-key")).thenReturn("sk-invalid");
        when(request.getRequestURI()).thenReturn("/v1/chat/completions");
        when(authenticationService.authenticateUser("sk-invalid"))
                .thenThrow(new AuthenticationFailedException("无效的 API Key"));
        StringWriter sw = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(sw));

        boolean result = interceptor.preHandle(request, response);

        assertThat(result).isFalse();
        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    }

    @Test
    @DisplayName("非代理路径直接放行")
    void nonProxyPath_passes() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/providers");

        boolean result = interceptor.preHandle(request, response);

        assertThat(result).isTrue();
    }
}
