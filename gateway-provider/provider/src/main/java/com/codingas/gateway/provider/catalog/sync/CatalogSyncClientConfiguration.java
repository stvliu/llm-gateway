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
package com.codingas.gateway.provider.catalog.sync;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.http.HttpClient;

/**
 * 模型目录同步客户端装配配置
 *
 * <p>提供 JDK 内置 {@link HttpClient} Bean，供 {@link ModelCatalogClient} 构造器注入
 * （provider 模块无 spring-web 依赖，不引入 RestClient/OkHttp）。
 * 由 {@code ProviderConfiguration} 的组件扫描自动注册。</p>
 */
@Configuration
public class CatalogSyncClientConfiguration {

    /**
     * JDK 内置 HTTP 客户端 Bean（供模型目录同步拉取使用）
     *
     * @return 默认配置的 {@link HttpClient} 实例
     */
    @Bean
    public HttpClient httpClient() {
        return HttpClient.newHttpClient();
    }
}
