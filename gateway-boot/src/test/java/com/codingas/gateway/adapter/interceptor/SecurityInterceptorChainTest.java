package com.codingas.gateway.adapter.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * SecurityInterceptorChain 单元测试
 */
@DisplayName("SecurityInterceptorChain 测试")
class SecurityInterceptorChainTest {

    private HttpServletRequest request;
    private HttpServletResponse response;

    @BeforeEach
    void setUp() {
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
    }

    @Nested
    @DisplayName("初始化测试")
    class InitTests {

        @Test
        @DisplayName("按 order 排序拦截器")
        void init_sortsByOrder() {
            // Given
            GatewayInterceptor first = createMockInterceptor("first", 1);
            GatewayInterceptor second = createMockInterceptor("second", 2);
            GatewayInterceptor third = createMockInterceptor("third", 3);

            // When - 乱序传入
            SecurityInterceptorChain chain = new SecurityInterceptorChain(
                    List.of(third, first, second));

            // Then - 按 order 排序
            List<GatewayInterceptor> interceptors = chain.getInterceptors();
            assertThat(interceptors).hasSize(3);
            assertThat(interceptors.get(0)).isEqualTo(first);
            assertThat(interceptors.get(1)).isEqualTo(second);
            assertThat(interceptors.get(2)).isEqualTo(third);
        }

        @Test
        @DisplayName("空拦截器列表")
        void init_emptyList() {
            // When
            SecurityInterceptorChain chain = new SecurityInterceptorChain(List.of());

            // Then
            assertThat(chain.getInterceptors()).isEmpty();
        }
    }

    @Nested
    @DisplayName("execute 测试")
    class ExecuteTests {

        @Test
        @DisplayName("所有拦截器通过返回 true")
        void execute_allPass_returnsTrue() throws Exception {
            // Given
            GatewayInterceptor first = createMockInterceptor("first", 1, true);
            GatewayInterceptor second = createMockInterceptor("second", 2, true);
            SecurityInterceptorChain chain = new SecurityInterceptorChain(
                    List.of(first, second));

            // When
            boolean result = chain.execute(request, response);

            // Then
            assertThat(result).isTrue();
            verify(first).preHandle(request, response);
            verify(second).preHandle(request, response);
        }

        @Test
        @DisplayName("任一拦截器拒绝返回 false")
        void execute_oneRejects_returnsFalse() throws Exception {
            // Given
            GatewayInterceptor first = createMockInterceptor("first", 1, true);
            GatewayInterceptor second = createMockInterceptor("second", 2, false);
            GatewayInterceptor third = createMockInterceptor("third", 3, true);
            SecurityInterceptorChain chain = new SecurityInterceptorChain(
                    List.of(first, second, third));

            // When
            boolean result = chain.execute(request, response);

            // Then
            assertThat(result).isFalse();
            verify(first).preHandle(request, response);
            verify(second).preHandle(request, response);
            verify(third, never()).preHandle(request, response);
        }

        @Test
        @DisplayName("第一个拦截器拒绝，后续不执行")
        void execute_firstRejects_shortCircuit() throws Exception {
            // Given
            GatewayInterceptor first = createMockInterceptor("first", 1, false);
            GatewayInterceptor second = createMockInterceptor("second", 2, true);
            SecurityInterceptorChain chain = new SecurityInterceptorChain(
                    List.of(first, second));

            // When
            boolean result = chain.execute(request, response);

            // Then
            assertThat(result).isFalse();
            verify(first).preHandle(request, response);
            verify(second, never()).preHandle(request, response);
        }

        @Test
        @DisplayName("空拦截器列表返回 true")
        void execute_emptyList_returnsTrue() throws Exception {
            // Given
            SecurityInterceptorChain chain = new SecurityInterceptorChain(List.of());

            // When
            boolean result = chain.execute(request, response);

            // Then
            assertThat(result).isTrue();
        }
    }

    // Helper methods
    private GatewayInterceptor createMockInterceptor(String name, int order) {
        return createMockInterceptor(name, order, true);
    }

    private GatewayInterceptor createMockInterceptor(String name, int order, boolean pass) {
        GatewayInterceptor interceptor = mock(GatewayInterceptor.class);
        when(interceptor.name()).thenReturn(name);
        when(interceptor.order()).thenReturn(order);
        try {
            when(interceptor.preHandle(any(), any())).thenReturn(pass);
        } catch (Exception e) {
            // ignore
        }
        return interceptor;
    }
}
