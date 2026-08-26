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
package com.codingas.gateway.web.api;

import com.codingas.gateway.web.advice.GlobalExceptionHandler;
import com.codingas.gateway.provider.channel.ChannelEmergencyManager;
import com.codingas.gateway.provider.channel.ChannelManager;
import com.codingas.gateway.provider.channel.ChannelHealthManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * ChannelController 应急操作端点契约测试
 *
 * <p>使用 standalone setup + Mock 各 Service，验证一键熔断/恢复/状态查询
 * 端点的路由、HTTP 状态码与响应契约，不连数据库。</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ChannelController 应急操作端点契约")
class ChannelEmergencyControllerTest {

    @Mock
    private com.codingas.gateway.web.api.assembler.ChannelFacade channelFacade;

    @Mock
    private ChannelManager channelManager;

    @Mock
    private ChannelHealthManager channelHealthManager;

    @Mock
    private ChannelEmergencyManager channelEmergencyManager;

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        ChannelController controller = new ChannelController(channelFacade, channelManager,
                channelHealthManager, channelEmergencyManager);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /channels/{cid}/endpoints/{eid}/circuit-breaker/force-open 一键熔断返回 200 与 OPEN")
    void forceOpen_returns200WithOpen() throws Exception {
        when(channelEmergencyManager.forceOpen(eq(1L), eq(10L))).thenReturn("OPEN");

        mockMvc.perform(post("/api/v1/channels/1/endpoints/10/circuit-breaker/force-open"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("OPEN"));
        verify(channelEmergencyManager).forceOpen(1L, 10L);
    }

    @Test
    @DisplayName("POST /channels/{cid}/endpoints/{eid}/circuit-breaker/force-close 一键恢复返回 200 与 CLOSED")
    void forceClose_returns200WithClosed() throws Exception {
        when(channelEmergencyManager.forceClose(eq(1L), eq(10L))).thenReturn("CLOSED");

        mockMvc.perform(post("/api/v1/channels/1/endpoints/10/circuit-breaker/force-close"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("CLOSED"));
        verify(channelEmergencyManager).forceClose(1L, 10L);
    }

    @Test
    @DisplayName("GET /channels/{cid}/endpoints/{eid}/circuit-breaker/state 查询状态返回 200")
    void getState_returns200WithState() throws Exception {
        when(channelEmergencyManager.getState(eq(1L), eq(10L))).thenReturn("HALF_OPEN");

        mockMvc.perform(get("/api/v1/channels/1/endpoints/10/circuit-breaker/state"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("HALF_OPEN"));
    }
}
