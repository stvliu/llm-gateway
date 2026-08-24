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

import com.codingas.gateway.security.threat.IpBlocklistDomainService;
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
    private IpBlocklistDomainService ipBlocklistService;

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
    @DisplayName("order返回0，最先执行")
    void order_returns0() {
        assertThat(interceptor.order()).isEqualTo(0);
    }

    private static class MockPrintWriter extends java.io.PrintWriter {
        public MockPrintWriter() { super(java.io.OutputStream.nullOutputStream()); }
    }
}
