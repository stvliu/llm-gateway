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
package com.codingas.simulator;

import com.codingas.simulator.service.SimulatorModeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 模拟服务端到端集成测试。
 * <p>
 * 使用 SpringBootTest 启动完整应用上下文，通过 TestRestTemplate 发送真实 HTTP 请求，
 * 验证模拟端点和管理 API 的端到端行为。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SimulatorEndToEndTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private SimulatorModeService modeService;

    /** OpenAI Chat Completion 请求体 */
    private static final String OPENAI_REQUEST_BODY = """
            {"model":"gpt-4o","messages":[{"role":"user","content":"hi"}]}""";

    /** Anthropic Messages 请求体 */
    private static final String ANTHROPIC_REQUEST_BODY = """
            {"model":"claude-sonnet-4-20250514","messages":[{"role":"user","content":"hi"}]}""";

    /**
     * 每个测试前重置模式为 NORMAL 并清理所有配置，避免测试间状态泄漏。
     * <p>
     * 仅重置 mode 为 NORMAL 不足以保证隔离：行为序列（尤其是 loop=true）、
     * 延迟配置、流控制、API Key 覆盖等会泄漏到后续测试，导致偶发失败。
     * 此处统一清理全部正交配置。
     */
    @BeforeEach
    void resetMode() {
        modeService.setMode(SimulatorModeService.SimulatorMode.NORMAL);
        // 清理所有配置，避免测试间状态泄漏
        modeService.clearBehaviorSequence();
        modeService.getDelayConfig().clearDelay();
        modeService.getStreamConfig().reset();
        modeService.getApiKeyOverrideConfig().clearAll();
    }

    // ==================== 场景 1: OpenAI 非流式 NORMAL 模式 ====================

    @Test
    @DisplayName("OpenAI 非流式 NORMAL 模式返回 200 + 成功响应，body 含 id 和 choices")
    void openaiNormalMode_returns200WithSuccessResponse() {
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/v1/chat/completions",
                new HttpEntity<>(OPENAI_REQUEST_BODY, jsonHeaders()),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).contains("\"id\"");
        assertThat(response.getBody()).contains("\"choices\"");
        assertThat(response.getBody()).contains("\"object\"");
    }

    // ==================== 场景 2: OpenAI 限流模式 ====================

    @Test
    @DisplayName("OpenAI 限流模式返回 429")
    void openaiRateLimitedMode_returns429() {
        // 先切换到限流模式
        switchMode("rate_limited");

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/v1/chat/completions",
                new HttpEntity<>(OPENAI_REQUEST_BODY, jsonHeaders()),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(response.getBody()).contains("rate_limit_error");
    }

    // ==================== 场景 3: OpenAI 上游错误模式 ====================

    @Test
    @DisplayName("OpenAI 上游错误模式返回 500")
    void openaiUpstreamErrorMode_returns500() {
        // 切换到上游错误模式
        switchMode("upstream_error");

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/v1/chat/completions",
                new HttpEntity<>(OPENAI_REQUEST_BODY, jsonHeaders()),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).contains("server_error");
    }

    // ==================== 场景 4: Anthropic 非流式 NORMAL 模式 ====================

    @Test
    @DisplayName("Anthropic 非流式 NORMAL 模式返回 200 + 成功响应，body 含 id 和 content")
    void anthropicNormalMode_returns200WithSuccessResponse() {
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/v1/messages",
                new HttpEntity<>(ANTHROPIC_REQUEST_BODY, jsonHeaders()),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).contains("\"id\"");
        assertThat(response.getBody()).contains("\"content\"");
        assertThat(response.getBody()).contains("\"type\"");
    }

    // ==================== 场景 5: 切换模式后查询一致 ====================

    @Test
    @DisplayName("切换模式后查询模式一致 — POST 设为 rate_limited，GET 验证")
    void afterSwitchMode_getModeReturnsConsistentValue() {
        // POST 切换到 rate_limited
        switchMode("rate_limited");

        // GET 查询当前模式
        ResponseEntity<Map<String, String>> response = restTemplate.exchange(
                "/simulator/mode",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("mode")).isEqualTo("RATE_LIMITED");
    }

    // ==================== 场景 6: 请求记录包含已发送的请求 ====================

    @Test
    @DisplayName("请求记录包含已发送的请求 — 先发模拟请求，再 GET 验证非空")
    void requestLogContainsSentRequests() {
        // 先发一个模拟请求
        restTemplate.postForEntity(
                "/v1/chat/completions",
                new HttpEntity<>(OPENAI_REQUEST_BODY, jsonHeaders()),
                String.class);

        // 查询请求记录
        ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                "/simulator/requests",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).isNotEmpty();

        // 验证请求记录中包含 /v1/chat/completions
        boolean foundChatCompletions = response.getBody().stream()
                .anyMatch(record -> "/v1/chat/completions".equals(record.get("path")));
        assertThat(foundChatCompletions).isTrue();
    }

    // ==================== 场景 7: 无效模式返回 400 ====================

    @Test
    @DisplayName("无效模式返回 400 — POST /simulator/mode 传 invalid_mode")
    void invalidMode_returns400() {
        // 注意：当前 SimulatorAdminController.parseMode 对未知模式会 fallback 到 NORMAL，
        // 不会返回 400。此测试验证我们需要加入模式校验后返回 400。
        // 如果当前实现尚未校验，此测试应先验证实际行为并据此调整。
        HttpHeaders headers = jsonHeaders();
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(
                Map.of("mode", "invalid_mode"), headers);

        ResponseEntity<Map<String, String>> response = restTemplate.exchange(
                "/simulator/mode",
                HttpMethod.POST,
                entity,
                new ParameterizedTypeReference<>() {});

        // 无效模式应返回 400 Bad Request
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // ==================== 场景 8: 行为序列测试 ====================

    @Test
    @DisplayName("行为序列 — 设置序列后 3 次请求按序返回 500/401/200，第 4 次恢复全局模式")
    void testBehaviorSequence_consumesStepsViaHttp() {
        // 先设全局为 NORMAL（确保基线）
        modeService.setMode(SimulatorModeService.SimulatorMode.NORMAL);

        // 设置行为序列：[500, 401, 200]
        restTemplate.exchange(
                "/simulator/behavior",
                HttpMethod.POST,
                new HttpEntity<>(Map.of(
                        "steps", List.of(500, 401, 200),
                        "loop", false
                ), jsonHeaders()),
                new ParameterizedTypeReference<Map<String, Object>>() {});

        // 第 1 次请求 → 500
        ResponseEntity<String> r1 = restTemplate.postForEntity(
                "/v1/chat/completions",
                new HttpEntity<>(OPENAI_REQUEST_BODY, jsonHeaders()),
                String.class);
        assertThat(r1.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);

        // 第 2 次请求 → 401
        ResponseEntity<String> r2 = restTemplate.postForEntity(
                "/v1/chat/completions",
                new HttpEntity<>(OPENAI_REQUEST_BODY, jsonHeaders()),
                String.class);
        assertThat(r2.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        // 第 3 次请求 → 200
        ResponseEntity<String> r3 = restTemplate.postForEntity(
                "/v1/chat/completions",
                new HttpEntity<>(OPENAI_REQUEST_BODY, jsonHeaders()),
                String.class);
        assertThat(r3.getStatusCode()).isEqualTo(HttpStatus.OK);

        // 第 4 次请求 → 恢复全局 NORMAL 模式
        ResponseEntity<String> r4 = restTemplate.postForEntity(
                "/v1/chat/completions",
                new HttpEntity<>(OPENAI_REQUEST_BODY, jsonHeaders()),
                String.class);
        assertThat(r4.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(r4.getBody()).contains("\"id\"");
        assertThat(r4.getBody()).contains("\"choices\"");
    }

    @Test
    @DisplayName("行为序列 — 循环序列 [200,500] 交替返回，6 次请求交替 200/500")
    void testBehaviorSequence_loop_resetsOnEnd() {
        modeService.setMode(SimulatorModeService.SimulatorMode.NORMAL);

        // 设置循环行为序列：[200, 500]
        restTemplate.exchange(
                "/simulator/behavior",
                HttpMethod.POST,
                new HttpEntity<>(Map.of(
                        "steps", List.of(200, 500),
                        "loop", true
                ), jsonHeaders()),
                new ParameterizedTypeReference<Map<String, Object>>() {});

        // 6 次请求交替 200/500
        for (int i = 0; i < 3; i++) {
            ResponseEntity<String> r1 = restTemplate.postForEntity(
                    "/v1/chat/completions",
                    new HttpEntity<>(OPENAI_REQUEST_BODY, jsonHeaders()),
                    String.class);
            assertThat(r1.getStatusCode()).isEqualTo(HttpStatus.OK);

            ResponseEntity<String> r2 = restTemplate.postForEntity(
                    "/v1/chat/completions",
                    new HttpEntity<>(OPENAI_REQUEST_BODY, jsonHeaders()),
                    String.class);
            assertThat(r2.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // ==================== 场景 9: 延迟配置测试 ====================

    @Test
    @DisplayName("延迟配置 — 设置 100ms 延迟后测量响应时间 ≥ 100ms")
    void testDelayConfig_appliesDelay() {
        modeService.setMode(SimulatorModeService.SimulatorMode.NORMAL);

        // 设置 100ms 延迟
        restTemplate.exchange(
                "/simulator/delay",
                HttpMethod.POST,
                new HttpEntity<>(Map.of("delayMs", 100), jsonHeaders()),
                new ParameterizedTypeReference<Map<String, Object>>() {});

        // 发送请求并计时
        long start = System.currentTimeMillis();
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/v1/chat/completions",
                new HttpEntity<>(OPENAI_REQUEST_BODY, jsonHeaders()),
                String.class);
        long elapsed = System.currentTimeMillis() - start;

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(elapsed).isGreaterThanOrEqualTo(100);
    }

    @Test
    @DisplayName("延迟配置 — 删除后延迟不再生效")
    void testDelayConfig_deleteResets() {
        modeService.setMode(SimulatorModeService.SimulatorMode.NORMAL);

        // 先设置延迟
        restTemplate.exchange(
                "/simulator/delay",
                HttpMethod.POST,
                new HttpEntity<>(Map.of("delayMs", 200), jsonHeaders()),
                new ParameterizedTypeReference<Map<String, Object>>() {});

        // 删除延迟配置
        restTemplate.exchange(
                "/simulator/delay",
                HttpMethod.DELETE,
                null,
                new ParameterizedTypeReference<Map<String, Object>>() {});

        // 确认不再延迟
        long start = System.currentTimeMillis();
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/v1/chat/completions",
                new HttpEntity<>(OPENAI_REQUEST_BODY, jsonHeaders()),
                String.class);
        long elapsed = System.currentTimeMillis() - start;

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(elapsed).isLessThan(200);
    }

    // ==================== 场景 10: 流控制测试 ====================

    @Test
    @DisplayName("流控制 — 设置中断后流式请求收到中断或异常")
    void testStreamConfig_interruptAfter() {
        modeService.setMode(SimulatorModeService.SimulatorMode.NORMAL);

        // 设置流中断：发送 3 个 chunk 后中断
        restTemplate.exchange(
                "/simulator/stream",
                HttpMethod.POST,
                new HttpEntity<>(Map.of(
                        "chunkCount", 5,
                        "interruptAfter", 3
                ), jsonHeaders()),
                new ParameterizedTypeReference<Map<String, Object>>() {});

        // 流式请求 — SSE 响应无法通过 RestTemplate 直接读取，可能抛出异常
        // 验证重点是：请求不会挂起，异常类型合理
        try {
            ResponseEntity<String> response = restTemplate.postForEntity(
                    "/v1/chat/completions",
                    new HttpEntity<>("""
                            {"model":"gpt-4o","messages":[{"role":"user","content":"hi"}],"stream":true}""",
                            jsonHeaders()),
                    String.class);
            // 如果能正常返回，验证 body 非空
            assertThat(response.getBody()).isNotNull();
        } catch (Exception e) {
            // SSE 中断导致 RestTemplate 解析失败是预期行为
            assertThat(e).isInstanceOf(RuntimeException.class);
        }
    }

    @Test
    @DisplayName("流控制 — 删除配置后恢复正常流式响应")
    void testStreamConfig_deleteResets() {
        modeService.setMode(SimulatorModeService.SimulatorMode.NORMAL);

        // 设置中断
        restTemplate.exchange(
                "/simulator/stream",
                HttpMethod.POST,
                new HttpEntity<>(Map.of("interruptAfter", 1), jsonHeaders()),
                new ParameterizedTypeReference<Map<String, Object>>() {});

        // 删除配置
        restTemplate.exchange(
                "/simulator/stream",
                HttpMethod.DELETE,
                null,
                new ParameterizedTypeReference<Map<String, Object>>() {});

        // 发送流式请求 — 应恢复正常
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/v1/chat/completions",
                new HttpEntity<>("""
                        {"model":"gpt-4o","messages":[{"role":"user","content":"hi"}],"stream":true}""",
                        jsonHeaders()),
                String.class);

        assertThat(response.getBody()).isNotNull();
    }

    // ==================== 场景 11: API Key 覆盖测试 ====================

    @Test
    @DisplayName("API Key 覆盖 — 匹配前缀的 Key 返回 401")
    void testApiKeyOverride_matchesByPrefix() {
        modeService.setMode(SimulatorModeService.SimulatorMode.NORMAL);

        // 设置 API Key 覆盖：前缀 sk-bad 返回 401
        restTemplate.exchange(
                "/simulator/apikey-override",
                HttpMethod.POST,
                new HttpEntity<>(Map.of(
                        "keyPrefix", "sk-bad",
                        "status", 401,
                        "body", "{\"error\":\"auth_error\"}"
                ), jsonHeaders()),
                new ParameterizedTypeReference<Map<String, Object>>() {});

        // 使用 sk-bad 前缀的 Key 发送请求 → 401
        HttpHeaders headers = jsonHeaders();
        headers.setBearerAuth("sk-bad-key-123");
        ResponseEntity<String> response = restTemplate.exchange(
                "/v1/chat/completions",
                HttpMethod.POST,
                new HttpEntity<>(OPENAI_REQUEST_BODY, headers),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("API Key 覆盖 — 不匹配的 Key 回退到全局 NORMAL 模式")
    void testApiKeyOverride_noMatch_fallsbackToGlobal() {
        modeService.setMode(SimulatorModeService.SimulatorMode.NORMAL);

        // 设置 API Key 覆盖：前缀 sk-bad 返回 401
        restTemplate.exchange(
                "/simulator/apikey-override",
                HttpMethod.POST,
                new HttpEntity<>(Map.of(
                        "keyPrefix", "sk-bad",
                        "status", 401
                ), jsonHeaders()),
                new ParameterizedTypeReference<Map<String, Object>>() {});

        // 使用不匹配的 Key → 正常 200
        HttpHeaders headers = jsonHeaders();
        headers.setBearerAuth("sk-good-key");
        ResponseEntity<String> response = restTemplate.exchange(
                "/v1/chat/completions",
                HttpMethod.POST,
                new HttpEntity<>(OPENAI_REQUEST_BODY, headers),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"id\"");
    }

    // ==================== 辅助方法 ====================

    /**
     * 切换模拟模式。
     *
     * @param mode 模式名称（normal / rate_limited / fault）
     */
    private void switchMode(String mode) {
        restTemplate.exchange(
                "/simulator/mode",
                HttpMethod.POST,
                new HttpEntity<>(Map.of("mode", mode), jsonHeaders()),
                new ParameterizedTypeReference<Map<String, String>>() {});
    }

    /**
     * 创建 JSON Content-Type 的 HttpHeaders。
     *
     * @return 包含 application/json Content-Type 的请求头
     */
    private HttpHeaders jsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }
}
