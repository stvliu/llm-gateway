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
package com.codingas.gateway.boot.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 网关启动装配配置属性
 *
 * <p>从 application.yml 读取网关启动相关配置。
 * 各域/承载层配置已下沉自持：CORS（gateway-web 的 {@code gateway.cors}）、
 * 限流（security 域的 {@code gateway.security.rate-limit}）等不再聚合于此。</p>
 */
@Component
@ConfigurationProperties(prefix = "gateway")
@Getter
@Setter
public class GatewayProperties {

    private InitProperties init = new InitProperties();

    @Getter
    @Setter
    public static class InitProperties {
        private boolean demoDataEnabled = false;
    }
}
