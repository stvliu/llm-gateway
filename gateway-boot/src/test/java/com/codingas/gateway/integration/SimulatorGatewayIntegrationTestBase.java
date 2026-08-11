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
package com.codingas.gateway.integration;

import com.codingas.gateway.GatewayApplication;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;

/**
 * Gateway 全链路集成测试基类。
 * <p>
 * 启动完整 Gateway Spring Boot 上下文（H2 内存数据库，禁用 Flyway），
 * 通过 TestRestTemplate 调用 Gateway API，Gateway 转发请求到上游。
 * 上游使用 ProviderSimulator（MockWebServer）模拟各种场景。
 */
@SpringBootTest(
    classes = GatewayApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("test")
public abstract class SimulatorGatewayIntegrationTestBase {

    @Autowired
    protected TestRestTemplate restTemplate;

    @LocalServerPort
    protected int gatewayPort;

    protected String gatewayUrl;

    @BeforeEach
    void setUp() {
        gatewayUrl = "http://localhost:" + gatewayPort;
    }

    /**
     * 创建包含 JSON Content-Type 和 API Key 的请求头。
     *
     * @param apiKey API 密钥
     * @return 请求头
     */
    protected HttpHeaders createHeaders(String apiKey) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);
        return headers;
    }

    /**
     * 创建标准 OpenAI Chat Completion 请求体。
     *
     * @param model 模型名称
     * @return 请求体 JSON 字符串
     */
    protected String createChatBody(String model) {
        return """
                {"model":"%s","messages":[{"role":"user","content":"hi"}]}""".formatted(model);
    }

    /**
     * 创建 OpenAI 流式请求体。
     *
     * @param model 模型名称
     * @return 请求体 JSON 字符串
     */
    protected String createStreamBody(String model) {
        return """
                {"model":"%s","messages":[{"role":"user","content":"hi"}],"stream":true}""".formatted(model);
    }
}
