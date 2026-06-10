package com.codingas.gateway.infrastructure.resilience;

/**
 * 重试策略接口
 *
 * <p>根据重试次数计算退避时间。</p>
 */
public interface RetryStrategy {

    /**
     * 计算第 N 次重试的退避时间
     *
     * @param attempt 当前重试次数（从 1 开始）
     * @return 退避时间（毫秒）
     */
    long calculateDelay(int attempt);

    /**
     * 最大重试次数
     */
    int maxAttempts();
}
