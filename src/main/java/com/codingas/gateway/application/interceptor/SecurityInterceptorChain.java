package com.codingas.gateway.application.interceptor;

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
