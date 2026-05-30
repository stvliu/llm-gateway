package com.codingas.gateway.infrastructure.resilience;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.function.Supplier;

/**
 * 重试执行器
 *
 * <p>基于指数退避策略执行带重试的操作。</p>
 */
@Component
public class RetryExecutor {

    private static final Logger log = LoggerFactory.getLogger(RetryExecutor.class);

    private final int maxAttempts;
    private final long backoffInitial;
    private final double backoffMultiplier;
    private final Set<Integer> retryableStatusCodes;

    public RetryExecutor(GatewayRetryProperties properties) {
        this.maxAttempts = properties.getMaxAttempts();
        this.backoffInitial = properties.getBackoffInitial();
        this.backoffMultiplier = properties.getBackoffMultiplier();
        this.retryableStatusCodes = properties.getRetryableStatusCodes();
    }

    /**
     * 执行带重试的操作
     *
     * @param action 待执行操作
     * @param <T> 返回类型
     * @return 操作结果
     */
    public <T> T execute(Supplier<T> action) {
        Exception lastException = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return action.get();
            } catch (Exception e) {
                lastException = e;
                if (!isRetryable(e) || attempt == maxAttempts) {
                    throw e;
                }
                long delay = calculateDelay(attempt);
                log.warn("重试 {}/{}，{}ms 后重试: {}", attempt, maxAttempts, delay, e.getMessage());
                sleep(delay);
            }
        }
        throw new RuntimeException("重试耗尽", lastException);
    }

    boolean isRetryable(Exception e) {
        if (e instanceof RetryableException) return true;
        String message = e.getMessage();
        if (message == null) return false;
        return retryableStatusCodes.stream()
            .anyMatch(code -> message.contains(String.valueOf(code)));
    }

    long calculateDelay(int attempt) {
        return (long) (backoffInitial * Math.pow(backoffMultiplier, attempt - 1));
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("重试等待被中断", e);
        }
    }
}