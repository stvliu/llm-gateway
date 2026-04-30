package com.codingas.gateway.infrastructure.advice;

import com.codingas.gateway.domain.security.service.SensitiveDataMasker;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 响应数据脱敏处理 (Spring MVC 版本)
 *
 * <p>对响应体中的敏感数据进行脱敏处理。</p>
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class MaskingResponseAdvice implements Filter {

    private final SensitiveDataMasker sensitiveDataMasker;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String path = httpRequest.getRequestURI();
        if (!shouldMask(path)) {
            chain.doFilter(request, response);
            return;
        }

        chain.doFilter(request, response);
    }

    private boolean shouldMask(String path) {
        return path.startsWith("/v1/") || path.startsWith("/anthropic/v1/");
    }

    @Override
    public void init(FilterConfig filterConfig) {
    }

    @Override
    public void destroy() {
    }
}