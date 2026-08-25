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
package com.codingas.gateway.security.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 限流外部配置属性（security 域自持）
 *
 * <p>由 security-starter 映射为 threat 域 {@link com.codingas.gateway.security.threat.RateLimitProperties}
 * 值对象 Bean，供限流领域服务注入。前缀 {@code gateway.security.rate-limit}。</p>
 */
@Data
@ConfigurationProperties(prefix = "gateway.security.rate-limit")
public class SecurityRateLimitProperties {

    /** 令牌桶容量 */
    private int bucketSize = 100;

    /** 令牌补充速率（个/秒） */
    private int refillRate = 10;

    /** fail-close 触发的 QPS 阈值 */
    private int qpsThreshold = 1000;
}
