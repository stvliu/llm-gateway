/*
 * Copyright (c) 2025 codingas.com
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE file for details.
 */
package com.codingas.gateway.adapter.interceptor;

import com.codingas.gateway.domain.threat.service.IpBlocklistDomainService;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * IP 封锁检查拦截器
 *
 * <p>责任链第一个拦截器，在认证前检查 IP 是否被封锁。</p>
 * <p>对于 SSE 异步请求，在异步分发阶段跳过检查，因为初始请求已验证。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IPBlockCheckInterceptor extends AbstractGatewayInterceptor {

    private final IpBlocklistDomainService ipBlocklistService;

    @Override
    public String name() {
        return "IPBlockCheck";
    }

    @Override
    public int order() {
        return 0; // 最先执行
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response) {
        // 异步分发阶段跳过 IP 检查
        if (request.getDispatcherType() == DispatcherType.ASYNC) {
            log.debug("Async dispatch, skipping IP block check: {}", request.getRequestURI());
            return true;
        }

        String clientIp = getClientIp(request);
        log.debug("Checking IP block for: {}", clientIp);

        if (ipBlocklistService.isBlocked(clientIp)) {
            log.warn("Blocked IP access: ip={}, uri={}", clientIp, request.getRequestURI());
            try {
                reject(response, "Access denied: IP blocked");
            } catch (Exception e) {
                log.error("Failed to write rejection response", e);
            }
            return false;
        }

        return true;
    }
}
