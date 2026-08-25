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
package com.codingas.gateway.web.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * CORS 跨域配置属性（gateway-web 承载层自持）
 *
 * <p>由 {@link CorsConfig} 消费，前缀 {@code gateway.cors}。</p>
 */
@Data
@ConfigurationProperties(prefix = "gateway.cors")
public class CorsProperties {

    /** 允许的源（逗号分隔） */
    private String allowedOrigins = "*";

    /** 允许的 HTTP 方法（逗号分隔） */
    private String allowedMethods = "GET,POST,PUT,PATCH,DELETE,OPTIONS";

    /** 允许的请求头（逗号分隔） */
    private String allowedHeaders = "*";

    /** 是否允许携带凭据 */
    private boolean allowCredentials = true;

    /** 预检请求缓存时间（秒） */
    private long maxAge = 3600;
}
