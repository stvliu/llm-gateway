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
package com.codingas.gateway.infrastructure.config;

import com.codingas.gateway.security.threat.RateLimitProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 限流配置装配。
 *
 * <p>把外部配置（{@link GatewayProperties#getRateLimit()}）映射为 threat 域定义的
 * {@link RateLimitProperties} 值对象 Bean，供限流领域服务注入 —— threat 域因此
 * 不反向依赖 boot 的配置包。</p>
 */
@Configuration
public class ThreatRateLimitConfig {

    /**
     * threat 域限流配置值对象
     */
    @Bean
    public RateLimitProperties rateLimitProperties(GatewayProperties gatewayProperties) {
        GatewayProperties.RateLimitProperties rl = gatewayProperties.getRateLimit();
        return new RateLimitProperties(rl.getBucketSize(), rl.getRefillRate(), rl.getQpsThreshold());
    }
}
