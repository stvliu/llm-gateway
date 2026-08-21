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

/**
 * 限流配置值对象（threat 域）。
 *
 * <p>令牌桶限流的可调参数，由 threat 域定义以解除对 boot 配置包的依赖；
 * 实际取值由组装层从外部配置绑定注入。</p>
 *
 * @param bucketSize   令牌桶容量
 * @param refillRate   令牌补充速率（个/秒）
 * @param qpsThreshold fail-close 触发的 QPS 阈值
 */
public record RateLimitProperties(int bucketSize, int refillRate, int qpsThreshold) {

    /** 默认限流配置（与历史默认一致：桶 100 / 补 10 / 阈值 1000） */
    public static final RateLimitProperties DEFAULT =
            new RateLimitProperties(100, 10, 1000);
}
