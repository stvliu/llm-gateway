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
package com.codingas.gateway.autoconfigure.security;

import com.codingas.gateway.security.config.SecurityRateLimitProperties;
import com.codingas.gateway.security.threat.RateLimitProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 限流配置装配（security-starter）。
 *
 * <p>把 security 域自持的外部配置（{@link SecurityRateLimitProperties}）映射为 threat 域定义的
 * {@link RateLimitProperties} 不可变对象 Bean，供限流管理服务注入。</p>
 */
@Configuration
public class SecurityRateLimitConfiguration {

    /**
     * threat 域限流配置不可变对象
     */
    @Bean
    public RateLimitProperties rateLimitProperties(SecurityRateLimitProperties properties) {
        return new RateLimitProperties(
                properties.getBucketSize(), properties.getRefillRate(), properties.getQpsThreshold());
    }
}
