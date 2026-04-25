package com.codingas.gateway.config.satoken;

import com.codingas.gateway.core.security.ipblock.IpBlocklistService;
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
@DisplayName("IPBlockCheckInterceptor Tests")
class IPBlockCheckInterceptorTest {

    @Mock
    private IpBlocklistService ipBlocklistService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    private IPBlockCheckInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new IPBlockCheckInterceptor(ipBlocklistService);
    }

    @Test
    @DisplayName("封锁IP返回false")
    void blockedIp_returnsFalse() throws Exception {
        // given
        when(request.getHeader("X-Forwarded-For")).thenReturn("192.168.1.100");
        when(ipBlocklistService.isBlocked("192.168.1.100")).thenReturn(true);
        when(response.getWriter()).thenReturn(new MockPrintWriter());

        // when
        boolean result = interceptor.preHandle(request, response);

        // then
        assertThat(result).isFalse();
        verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
    }

    @Test
    @DisplayName("正常IP通过检查")
    void normalIp_returnsTrue() throws Exception {
        // given
        when(request.getHeader("X-Forwarded-For")).thenReturn("10.0.0.1");
        when(ipBlocklistService.isBlocked("10.0.0.1")).thenReturn(false);

        // when
        boolean result = interceptor.preHandle(request, response);

        // then
        assertThat(result).isTrue();
        verify(response, never()).setStatus(anyInt());
    }

    @Test
    @DisplayName("order返回1，最先执行")
    void order_returns1() {
        assertThat(interceptor.order()).isEqualTo(1);
    }

    private static class MockPrintWriter extends java.io.PrintWriter {
        public MockPrintWriter() { super(java.io.OutputStream.nullOutputStream()); }
    }
}