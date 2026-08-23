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
package com.codingas.gateway.resilience.retry;

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
