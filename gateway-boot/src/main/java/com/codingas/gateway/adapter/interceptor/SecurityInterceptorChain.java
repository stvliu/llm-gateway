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

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

/**
 * 安全拦截器责任链管理器
 *
 * <p>管理和执行所有 GatewayInterceptor 实现，按 order 排序执行。</p>
 */
@Slf4j
@Component
public class SecurityInterceptorChain {

    private final List<GatewayInterceptor> interceptors;

    public SecurityInterceptorChain(List<GatewayInterceptor> interceptors) {
        // 按 order 排序，数字小的先执行
        this.interceptors = interceptors.stream()
                .sorted(Comparator.comparingInt(GatewayInterceptor::order))
                .toList();
        log.info("SecurityInterceptorChain initialized with {} interceptors: {}",
                this.interceptors.size(),
                this.interceptors.stream().map(GatewayInterceptor::name).toList());
    }

    /**
     * 执行责任链
     *
     * @return true 所有拦截器都通过，false 任一拦截器拒绝
     */
    public boolean execute(HttpServletRequest request, HttpServletResponse response) throws Exception {
        for (GatewayInterceptor interceptor : interceptors) {
            log.debug("Executing interceptor: {}", interceptor.name());
            if (!interceptor.preHandle(request, response)) {
                log.debug("Interceptor {} rejected request", interceptor.name());
                return false;
            }
        }
        return true;
    }

    /**
     * 获取当前拦截器列表（用于测试）
     */
    public List<GatewayInterceptor> getInterceptors() {
        return interceptors;
    }
}
