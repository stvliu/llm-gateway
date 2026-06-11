package com.codingas.gateway.infrastructure.resilience;

import com.codingas.gateway.domain.supply.enums.ProviderErrorType;
import com.codingas.gateway.domain.supply.exception.ProviderException;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.function.Supplier;

/**
 * 重试执行器
 *
 * <p>基于策略模式执行带重试的操作，根据异常类型选择对应的重试策略。</p>
 */
@Component
public class RetryExecutor {

    private static final Logger log = LoggerFactory.getLogger(RetryExecutor.class);

    private final GatewayRetryProperties properties;
    private final Set<Integer> retryableStatusCodes;
    private final MeterRegistry meterRegistry;

    public RetryExecutor(GatewayRetryProperties properties, MeterRegistry meterRegistry) {
        this.properties = properties;
        this.retryableStatusCodes = properties.getRetryableStatusCodes();
        this.meterRegistry = meterRegistry;
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

        // 第一次执行
        try {
            return action.get();
        } catch (Exception e) {
            lastException = e;
            if (!isRetryable(e)) {
                throw e;
            }
        }

        // 根据异常类型选择策略
        RetryStrategy strategy = selectStrategy(lastException);
        int maxAttempts = strategy.maxAttempts();

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return action.get();
            } catch (Exception e) {
                lastException = e;
                recordRetryAttempt(e, attempt);
                if (!isRetryable(e) || attempt == maxAttempts) {
                    meterRegistry.counter("gateway.retry.exhausted",
                            "error_type", extractErrorType(e)).increment();
                    throw e;
                }
                long delay = strategy.calculateDelay(attempt);
                log.warn("重试 {}/{}，{}ms 后重试: {}", attempt, maxAttempts, delay, e.getMessage());
                sleep(delay);
            }
        }
        throw new RuntimeException("重试耗尽", lastException);
    }

    /**
     * 记录重试 Metrics
     */
    private void recordRetryAttempt(Exception e, int attempt) {
        String errorType = extractErrorType(e);
        meterRegistry.counter("gateway.retry.attempts",
                "attempt", String.valueOf(attempt),
                "error_type", errorType).increment();
    }

    private String extractErrorType(Exception e) {
        if (e instanceof ProviderException pe && pe.getErrorType() != null) {
            return pe.getErrorType().name();
        }
        return "UNKNOWN";
    }

    /**
     * 根据异常类型选择对应的重试策略
     */
    private RetryStrategy selectStrategy(Exception e) {
        if (e instanceof ProviderException pe) {
            return switch (pe.getErrorType()) {
                case RATE_LIMIT_ERROR -> new RateLimitRetryStrategy(properties);
                case TIMEOUT_ERROR -> new FastRetryStrategy(properties);
                case UPSTREAM_ERROR, SERVICE_UNAVAILABLE -> new ServiceUnavailableStrategy(properties);
                default -> new ExponentialBackoffStrategy(properties);
            };
        }
        return new ExponentialBackoffStrategy(properties);
    }

    /**
     * 判断异常是否可重试
     */
    boolean isRetryable(Exception e) {
        if (e instanceof ProviderException pe) {
            return switch (pe.getErrorType()) {
                case QUOTA_EXCEEDED, AUTHENTICATION_ERROR, INVALID_REQUEST -> false;
                default -> true;
            };
        }
        if (e instanceof RetryableException) return true;
        String message = e.getMessage();
        if (message == null) return false;
        return retryableStatusCodes.stream()
            .anyMatch(code -> message.contains(String.valueOf(code)));
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
