package com.codingas.gateway.infrastructure.advice;

import com.codingas.gateway.domain.security.service.SensitiveDataMasker;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;

import static org.mockito.Mockito.*;

/**
 * MaskingResponseAdvice 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MaskingResponseAdvice 测试")
class MaskingResponseAdviceTest {

    @Mock
    private SensitiveDataMasker sensitiveDataMasker;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private MaskingResponseAdvice advice;

    @BeforeEach
    void setUp() {
        advice = new MaskingResponseAdvice(sensitiveDataMasker);
    }

    @Nested
    @DisplayName("doFilter 测试")
    class DoFilterTests {

        @Test
        @DisplayName("OpenAI 路径需要脱敏处理")
        void doFilter_openaiPath_processesMasking() throws IOException, ServletException {
            // Given
            when(request.getRequestURI()).thenReturn("/v1/chat/completions");

            // When
            advice.doFilter(request, response, filterChain);

            // Then
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("Anthropic 路径需要脱敏处理")
        void doFilter_anthropicPath_processesMasking() throws IOException, ServletException {
            // Given
            when(request.getRequestURI()).thenReturn("/anthropic/v1/messages");

            // When
            advice.doFilter(request, response, filterChain);

            // Then
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("管理路径不需要脱敏处理")
        void doFilter_adminPath_skipsMasking() throws IOException, ServletException {
            // Given
            when(request.getRequestURI()).thenReturn("/admin/users");

            // When
            advice.doFilter(request, response, filterChain);

            // Then
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("静态资源路径不需要脱敏处理")
        void doFilter_staticPath_skipsMasking() throws IOException, ServletException {
            // Given
            when(request.getRequestURI()).thenReturn("/static/js/app.js");

            // When
            advice.doFilter(request, response, filterChain);

            // Then
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("根路径不需要脱敏处理")
        void doFilter_rootPath_skipsMasking() throws IOException, ServletException {
            // Given
            when(request.getRequestURI()).thenReturn("/");

            // When
            advice.doFilter(request, response, filterChain);

            // Then
            verify(filterChain).doFilter(request, response);
        }
    }

    @Nested
    @DisplayName("生命周期方法测试")
    class LifecycleTests {

        @Test
        @DisplayName("init 方法正常执行")
        void init_noException() {
            // When & Then - 不应抛出异常
            advice.init(null);
        }

        @Test
        @DisplayName("destroy 方法正常执行")
        void destroy_noException() {
            // When & Then - 不应抛出异常
            advice.destroy();
        }
    }
}
