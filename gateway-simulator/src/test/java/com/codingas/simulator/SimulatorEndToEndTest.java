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
     * 每个测试前重置模式为 NORMAL，避免测试间状态泄漏。
     */
    @BeforeEach
    void resetMode() {
        modeService.setMode(SimulatorModeService.SimulatorMode.NORMAL);
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
