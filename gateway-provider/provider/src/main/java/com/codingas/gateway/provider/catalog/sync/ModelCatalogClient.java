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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * models.dev 模型目录数据源客户端
 *
 * <p>使用 JDK 内置 {@link HttpClient} 拉取 models.dev 主数据（provider 模块
 * 无 spring-web 依赖，不引入 RestClient/OkHttp），解析为 {@link ModelCatalogDto} 列表。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ModelCatalogClient {

    private final HttpClient httpClient;
    private final CatalogSyncProperties properties;
    private final ObjectMapper objectMapper;

    /**
     * 拉取并解析 models.dev 模型主数据
     *
     * @return 模型目录 DTO 列表
     * @throws CatalogSyncException 拉取或解析失败时
     */
    public List<ModelCatalogDto> fetch() {
        if (!properties.isEnabled()) {
            throw new CatalogSyncException("模型目录数据源已禁用", null);
        }
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(properties.getUrl()))
                    .timeout(properties.getTimeout())
                    .header("Accept", "application/json")
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new CatalogSyncException("模型目录拉取失败, HTTP " + response.statusCode(), null);
            }
            Map<String, ModelCatalogDto> catalog =
                    objectMapper.readValue(response.body(), new TypeReference<>() {});
            return new ArrayList<>(catalog.values());
        } catch (CatalogSyncException e) {
            throw e;
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CatalogSyncException("模型目录拉取失败: " + e.getMessage(), e);
        }
    }
}
