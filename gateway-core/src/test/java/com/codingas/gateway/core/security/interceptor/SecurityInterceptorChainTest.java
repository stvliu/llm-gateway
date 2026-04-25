package com.codingas.gateway.core.security.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SecurityInterceptorChain Tests")
class SecurityInterceptorChainTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Nested
    @DisplayName("execute")
    class ExecuteTests {

        @Test
        @DisplayName("所有拦截器通过时返回 true")
        void allPass_returnsTrue() throws Exception {
            // given
            GatewayInterceptor first = createPassInterceptor("First", 1);
            GatewayInterceptor second = createPassInterceptor("Second", 2);
            var chain = new SecurityInterceptorChain(List.of(first, second));

            // when
            boolean result = chain.execute(request, response);

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("第一个拦截器拒绝时返回 false")
        void firstRejects_returnsFalse() throws Exception {
            // given
            GatewayInterceptor rejecter = createRejectInterceptor("Rejecter", 1);
            GatewayInterceptor passer = createPassInterceptor("Passer", 2);
            var chain = new SecurityInterceptorChain(List.of(rejecter, passer));

            // when
            boolean result = chain.execute(request, response);

            // then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("按 order 排序执行")
        void sortedByOrder() throws Exception {
            // given
            var executionOrder = new java.util.ArrayList<String>();
            GatewayInterceptor last = createOrderTrackingInterceptor("Last", 3, executionOrder);
            GatewayInterceptor first = createOrderTrackingInterceptor("First", 1, executionOrder);
            GatewayInterceptor middle = createOrderTrackingInterceptor("Middle", 2, executionOrder);
            var chain = new SecurityInterceptorChain(List.of(last, first, middle));

            // when
            boolean result = chain.execute(request, response);

            // then - 验证执行顺序是按 order 排序的
            assertThat(result).isTrue();
            assertThat(executionOrder).containsExactly("First", "Middle", "Last");
        }
    }

    private GatewayInterceptor createPassInterceptor(String name, int order) {
        return new GatewayInterceptor() {
            @Override
            public String name() { return name; }
            @Override
            public boolean preHandle(HttpServletRequest req, HttpServletResponse resp) { return true; }
            @Override
            public int order() { return order; }
        };
    }

    private GatewayInterceptor createOrderTrackingInterceptor(String name, int order, java.util.ArrayList<String> executionOrder) {
        return new GatewayInterceptor() {
            @Override
            public String name() { return name; }
            @Override
            public boolean preHandle(HttpServletRequest req, HttpServletResponse resp) {
                executionOrder.add(name);
                return true;
            }
            @Override
            public int order() { return order; }
        };
    }

    private GatewayInterceptor createRejectInterceptor(String name, int order) {
        return new GatewayInterceptor() {
            @Override
            public String name() { return name; }
            @Override
            public boolean preHandle(HttpServletRequest req, HttpServletResponse resp) { return false; }
            @Override
            public int order() { return order; }
        };
    }
}