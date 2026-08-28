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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * ModelCatalogClient 单元测试
 *
 * <p>使用 JDK 内置 {@link HttpServer} 起本地 HTTP 服务返回样例 JSON，
 * 验证 {@link ModelCatalogClient#fetch()} 的拉取与解析行为。</p>
 */
@ExtendWith(MockitoExtension.class)
class ModelCatalogClientTest {

    private HttpServer server;
    private String baseUrl;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        byte[] body;
        try (InputStream in = getClass().getClassLoader()
                .getResourceAsStream("modelsdev-sample.json")) {
            if (in == null) {
                throw new IllegalStateException("测试资源 modelsdev-sample.json 不存在");
            }
            body = in.readAllBytes();
        }
        server.createContext("/models.json", exchange -> {
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (var os = exchange.getResponseBody()) { os.write(body); }
        });
        server.start();
        baseUrl = "http://localhost:" + server.getAddress().getPort() + "/models.json";
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    /**
     * 构造被测客户端（注入 JDK HttpClient 与带 JavaTimeModule 的 ObjectMapper）
     */
    private ModelCatalogClient newClient(String url) {
        CatalogSyncProperties props = new CatalogSyncProperties();
        props.setUrl(url);
        props.setTimeout(Duration.ofSeconds(5));
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        return new ModelCatalogClient(HttpClient.newHttpClient(), props, objectMapper);
    }

    @Test
    @DisplayName("拉取并解析 models.dev 模型数据")
    void fetch_parsesModels() {
        ModelCatalogClient client = newClient(baseUrl);

        List<ModelCatalogDto> models = client.fetch();

        assertThat(models).hasSize(3);
        ModelCatalogDto gpt4o = models.stream()
                .filter(m -> m.id().equals("openai/gpt-4o")).findFirst().orElseThrow();
        assertThat(gpt4o.name()).isEqualTo("GPT-4o");
        assertThat(gpt4o.attachment()).isTrue();
        assertThat(gpt4o.toolCall()).isTrue();
        assertThat(gpt4o.releaseDate()).isEqualTo("2024-05-13");
        assertThat(gpt4o.limit().context()).isEqualTo(128000L);
        assertThat(gpt4o.modalities().input()).containsExactly("text", "image");
        assertThat(gpt4o.benchmarks()).hasSize(1);

        ModelCatalogDto minimal = models.stream()
                .filter(m -> m.id().equals("minimal/minimal-model")).findFirst().orElseThrow();
        assertThat(minimal.limit().input()).isNull();
        assertThat(minimal.structuredOutput()).isNull();
    }

    @Test
    @DisplayName("HTTP 失败时抛出 CatalogSyncException")
    void fetch_serverError_throwsCatalogSyncException() {
        ModelCatalogClient client = newClient(baseUrl.replace("/models.json", "/missing"));

        assertThatThrownBy(client::fetch).isInstanceOf(CatalogSyncException.class);
    }
}
