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
package com.codingas.simulator.controller;

import com.codingas.simulator.service.SimulatorModeManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * SimulatorAdminController 集成测试。
 * <p>
 * 验证管理 API 的模式切换和请求记录查询功能。
 */
@WebMvcTest(SimulatorAdminController.class)
@Import(SimulatorModeManager.class)
class SimulatorAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SimulatorModeManager modeService;

    @BeforeEach
    void resetMode() {
        modeService.setMode(SimulatorModeManager.SimulatorMode.NORMAL);
    }

    @Nested
    @DisplayName("GET /simulator/mode — 获取当前模式")
    class GetMode {

        @Test
        @DisplayName("默认返回 normal 模式")
        void defaultMode_returnsNormal() throws Exception {
            mockMvc.perform(get("/simulator/mode"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.mode").value("NORMAL"));
        }

        @Test
        @DisplayName("切换后返回新模式")
        void afterSwitch_returnsNewMode() throws Exception {
            modeService.setMode(SimulatorModeManager.SimulatorMode.RATE_LIMITED);

            mockMvc.perform(get("/simulator/mode"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.mode").value("RATE_LIMITED"));
        }
    }

    @Nested
    @DisplayName("POST /simulator/mode — 切换模式")
    class SetMode {

        @Test
        @DisplayName("切换到 rate_limited 模式")
        void switchToRateLimited() throws Exception {
            mockMvc.perform(post("/simulator/mode")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"mode\":\"rate_limited\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.mode").value("RATE_LIMITED"));
        }

        @Test
        @DisplayName("切换到 upstream_error 模式")
        void switchToUpstreamError() throws Exception {
            mockMvc.perform(post("/simulator/mode")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"mode\":\"upstream_error\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.mode").value("UPSTREAM_ERROR"));
        }

        @Test
        @DisplayName("切换到 auth_error 模式")
        void switchToAuthError() throws Exception {
            mockMvc.perform(post("/simulator/mode")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"mode\":\"auth_error\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.mode").value("AUTH_ERROR"));
        }

        @Test
        @DisplayName("切换回 normal 模式")
        void switchToNormal() throws Exception {
            modeService.setMode(SimulatorModeManager.SimulatorMode.UPSTREAM_ERROR);

            mockMvc.perform(post("/simulator/mode")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"mode\":\"normal\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.mode").value("NORMAL"));
        }
    }

    @Nested
    @DisplayName("GET /simulator/requests — 查询请求记录")
    class GetRequests {

        @Test
        @DisplayName("无请求记录时返回空数组")
        void noRequests_returnsEmptyArray() throws Exception {
            mockMvc.perform(get("/simulator/requests"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(0));
        }

        @Test
        @DisplayName("有请求记录时返回记录列表")
        void hasRequests_returnsList() throws Exception {
            modeService.recordRequest("POST", "/v1/chat/completions");

            mockMvc.perform(get("/simulator/requests"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].method").value("POST"))
                    .andExpect(jsonPath("$[0].path").value("/v1/chat/completions"));
        }
    }
}
