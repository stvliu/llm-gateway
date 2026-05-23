package com.codingas.gateway.adapter.interceptor;

import com.codingas.gateway.domain.iam.exception.AuthenticationFailedException;
import com.codingas.gateway.domain.iam.service.AuthenticationDomainService;
import com.codingas.gateway.domain.iam.service.Identity;
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
        Identity identity = Identity.of(1L, "user", 1L);
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
        Identity identity = Identity.of(1L, "user", 1L);
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
