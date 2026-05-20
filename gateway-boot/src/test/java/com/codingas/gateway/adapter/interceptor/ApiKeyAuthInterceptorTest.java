package com.codingas.gateway.adapter.interceptor;

import com.codingas.gateway.domain.security.service.AuthenticationDomainService;
import com.codingas.gateway.domain.security.service.UserAuthResult;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
        // given
        when(request.getHeader("Authorization")).thenReturn("Bearer sk-test123");
        when(request.getRequestURI()).thenReturn("/v1/chat/completions");
        when(authenticationService.authenticate("sk-test123"))
                .thenReturn(UserAuthResult.legacy(1L, "USER", 1L));

        // when
        boolean result = interceptor.preHandle(request, response);

        // then
        assertThat(result).isTrue();
        verify(request).setAttribute("userId", 1L);
        verify(request).setAttribute("apiKeyId", 1L);
    }

    @Test
    @DisplayName("有效API Key通过检查 - X-API-Key header")
    void validApiKey_xApiKeyHeader_passes() throws Exception {
        // given
        when(request.getHeader("Authorization")).thenReturn(null);
        when(request.getHeader("X-API-Key")).thenReturn("sk-test123");
        when(request.getRequestURI()).thenReturn("/v1/chat/completions");
        when(authenticationService.authenticate("sk-test123"))
                .thenReturn(UserAuthResult.legacy(1L, "USER", 1L));

        // when
        boolean result = interceptor.preHandle(request, response);

        // then
        assertThat(result).isTrue();
        verify(request).setAttribute("userId", 1L);
        verify(request).setAttribute("apiKeyId", 1L);
    }

    @Test
    @DisplayName("缺少API Key返回401")
    void missingApiKey_returns401() throws Exception {
        // given
        when(request.getHeader("Authorization")).thenReturn(null);
        when(request.getHeader("X-API-Key")).thenReturn(null);
        when(request.getRequestURI()).thenReturn("/v1/chat/completions");
        when(response.getWriter()).thenReturn(new MockPrintWriter());

        // when
        boolean result = interceptor.preHandle(request, response);

        // then
        assertThat(result).isFalse();
        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    }

    @Test
    @DisplayName("无效API Key返回401")
    void invalidApiKey_returns401() throws Exception {
        // given
        when(request.getHeader("Authorization")).thenReturn(null);
        when(request.getHeader("X-API-Key")).thenReturn("sk-invalid");
        when(authenticationService.authenticate("sk-invalid")).thenReturn(null);
        when(request.getRequestURI()).thenReturn("/v1/chat/completions");
        when(response.getWriter()).thenReturn(new MockPrintWriter());

        // when
        boolean result = interceptor.preHandle(request, response);

        // then
        assertThat(result).isFalse();
        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    }

    @Test
    @DisplayName("order返回2，在IP检查之后")
    void order_returns2() {
        assertThat(interceptor.order()).isEqualTo(2);
    }

    private static class MockPrintWriter extends java.io.PrintWriter {
        public MockPrintWriter() { super(java.io.OutputStream.nullOutputStream()); }
    }
}
