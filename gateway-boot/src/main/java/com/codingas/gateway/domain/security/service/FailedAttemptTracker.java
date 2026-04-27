package com.codingas.gateway.domain.security.service;

/**
 * 失败尝试追踪器接口
 *
 * <p>实现可以是 Redis 版本（生产）或内存版本（开发）。</p>
 */
public interface FailedAttemptTracker {

    /**
     * 增加计数并返回当前值
     */
    long increment(String key);

    /**
     * 删除指定 key 的计数
     */
    void delete(String key);

    /**
     * 获取当前计数
     */
    int get(String key);
}
