package com.codingas.simulator.controller;

import com.codingas.simulator.service.SimulatorModeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * SimulatorController 集成测试。
 * <p>
 * 验证模拟端点在 NORMAL / RATE_LIMITED / FAULT 模式下的响应行为。
 */
@WebMvcTest(SimulatorController.class)
@Import(SimulatorModeService.class)
class SimulatorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SimulatorModeService modeService;

    @BeforeEach
    void resetMode() {
        modeService.setMode(SimulatorModeService.SimulatorMode.NORMAL);
    }

    // ==================== OpenAI 端点测试 ====================

    @Nested
    @DisplayName("OpenAI /v1/chat/completions 端点")
    class OpenAIEndpoint {

        @Test
        @DisplayName("NORMAL 模式下返回 200 和 Chat Completion JSON")
        void normalMode_returnsChatCompletion() throws Exception {
            mockMvc.perform(post("/v1/chat/completions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"model\":\"gpt-4o\",\"messages\":[{\"role\":\"user\",\"content\":\"hi\"}]}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.object").value("chat.completion"))
                    .andExpect(jsonPath("$.choices").isArray())
                    .andExpect(jsonPath("$.usage").exists());
        }

        @Test
        @DisplayName("NORMAL 模式下 stream=true 返回 SSE 事件流")
        void normalMode_stream_returnsSseEmitter() throws Exception {
            mockMvc.perform(post("/v1/chat/completions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"model\":\"gpt-4o\",\"messages\":[{\"role\":\"user\",\"content\":\"hi\"}],\"stream\":true}"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("RATE_LIMITED 模式下返回 429")
        void rateLimitedMode_returns429() throws Exception {
            modeService.setMode(SimulatorModeService.SimulatorMode.RATE_LIMITED);

            mockMvc.perform(post("/v1/chat/completions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"model\":\"gpt-4o\",\"messages\":[{\"role\":\"user\",\"content\":\"hi\"}]}"))
                    .andExpect(status().isTooManyRequests())
                    .andExpect(jsonPath("$.error.type").value("rate_limit_error"));
        }

        @Test
        @DisplayName("FAULT 模式下返回 500")
        void faultMode_returns500() throws Exception {
            modeService.setMode(SimulatorModeService.SimulatorMode.FAULT);

            mockMvc.perform(post("/v1/chat/completions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"model\":\"gpt-4o\",\"messages\":[{\"role\":\"user\",\"content\":\"hi\"}]}"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.error.type").value("server_error"));
        }
    }

    // ==================== Anthropic 端点测试 ====================

    @Nested
    @DisplayName("Anthropic /v1/messages 端点")
    class AnthropicEndpoint {

        @Test
        @DisplayName("NORMAL 模式下返回 200 和 Messages JSON")
        void normalMode_returnsMessages() throws Exception {
            mockMvc.perform(post("/v1/messages")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("anthropic-version", "2023-06-01")
                            .content("{\"model\":\"claude-sonnet-4-20250514\",\"messages\":[{\"role\":\"user\",\"content\":\"hi\"}]}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.type").value("message"))
                    .andExpect(jsonPath("$.role").value("assistant"))
                    .andExpect(jsonPath("$.content").isArray());
        }

        @Test
        @DisplayName("RATE_LIMITED 模式下返回 429")
        void rateLimitedMode_returns429() throws Exception {
            modeService.setMode(SimulatorModeService.SimulatorMode.RATE_LIMITED);

            mockMvc.perform(post("/v1/messages")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("anthropic-version", "2023-06-01")
                            .content("{\"model\":\"claude-sonnet-4-20250514\",\"messages\":[{\"role\":\"user\",\"content\":\"hi\"}]}"))
                    .andExpect(status().isTooManyRequests())
                    .andExpect(jsonPath("$.error.type").value("rate_limit_error"));
        }

        @Test
        @DisplayName("FAULT 模式下返回 500")
        void faultMode_returns500() throws Exception {
            modeService.setMode(SimulatorModeService.SimulatorMode.FAULT);

            mockMvc.perform(post("/v1/messages")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("anthropic-version", "2023-06-01")
                            .content("{\"model\":\"claude-sonnet-4-20250514\",\"messages\":[{\"role\":\"user\",\"content\":\"hi\"}]}"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.error.type").value("api_error"));
        }
    }
}
