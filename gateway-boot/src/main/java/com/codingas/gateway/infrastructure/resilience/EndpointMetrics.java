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
package com.codingas.gateway.infrastructure.resilience;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 端点级调用统计（参照 Dubbo RpcStatus）
 *
 * <p>为每个 ChannelEndpoint 维护活跃数、总调用次数、总耗时、失败次数。</p>
 * <p>线程安全，使用 Atomic 系列实现无锁统计。</p>
 */
public class EndpointMetrics {

    private final AtomicInteger active = new AtomicInteger(0);
    private final AtomicLong totalCalls = new AtomicLong(0);
    private final AtomicLong totalDuration = new AtomicLong(0);
    private final AtomicLong failedCalls = new AtomicLong(0);

    /**
     * 调用开始，活跃数 +1
     */
    public void beginCall() {
        active.incrementAndGet();
    }

    /**
     * 调用结束，更新统计
     *
     * @param durationMs 耗时（毫秒）
     * @param success    是否成功
     */
    public void endCall(long durationMs, boolean success) {
        active.decrementAndGet();
        totalCalls.incrementAndGet();
        totalDuration.addAndGet(durationMs);
        if (!success) {
            failedCalls.incrementAndGet();
        }
    }

    /** 当前活跃请求数 */
    public int getActive() { return active.get(); }

    /** 总调用次数 */
    public long getTotalCalls() { return totalCalls.get(); }

    /** 总耗时（毫秒） */
    public long getTotalDuration() { return totalDuration.get(); }

    /** 失败次数 */
    public long getFailedCalls() { return failedCalls.get(); }

    /** 平均耗时 */
    public double getAverageDuration() {
        long total = totalCalls.get();
        return total > 0 ? (double) totalDuration.get() / total : 0;
    }

    /** 失败率 */
    public double getFailureRate() {
        long total = totalCalls.get();
        return total > 0 ? (double) failedCalls.get() / total : 0;
    }
}
