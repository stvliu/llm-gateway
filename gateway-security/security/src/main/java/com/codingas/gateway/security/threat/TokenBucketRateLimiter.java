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
package com.codingas.gateway.security.threat;

import com.codingas.gateway.security.threat.TokenBucketStatus;

/**
 * 令牌桶限流器接口
 *
 * <p>实现可以是 Redis 版本（生产）或内存版本（开发）。</p>
 */
public interface TokenBucketRateLimiter {

    /**
     * 尝试获取令牌
     *
     * @param key        限流 key（如 user:123 或 api_key:xxx）
     * @param capacity   桶容量
     * @param refillRate 每秒补充令牌数
     * @param requested  请求令牌数（通常为1）
     * @return true 表示允许，false 表示被限流
     */
    boolean tryAcquire(String key, int capacity, int refillRate, int requested);

    /**
     * 获取当前令牌桶状态
     */
    TokenBucketStatus getStatus(String key, int capacity, int refillRate);

    /**
     * 重置限流桶
     */
    void reset(String key);
}
