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
import com.codingas.gateway.iam.application.Application;
import com.codingas.gateway.iam.application.ApplicationChannel;
import com.codingas.gateway.iam.application.ApplicationService;
import com.codingas.gateway.iam.application.ApplicationState;
import com.codingas.gateway.web.api.dto.ApplicationRequest;
import com.codingas.gateway.iam.apikey.UserApiKeyService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * ApplicationController HTTP 契约测试
 *
 * <p>使用 standalone setup + Mock ApplicationService，验证 REST 端点路由、
 * HTTP 状态码与响应序列化契约，不连数据库。</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ApplicationController 端点契约")
class ApplicationControllerHttpTest {

    @Mock
    private ApplicationService applicationService;

    @Mock
    private UserApiKeyService userApiKeyService;

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        ApplicationController controller = new ApplicationController(applicationService, userApiKeyService);
        // 装配 GlobalExceptionHandler 以便校验失败被转为 400（与生产 Web 上下文一致）
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    /**
     * 构造含完整字段的应用实体桩
     */
    private Application stubApp() {
        Application app = new Application();
        app.setId(1L);
        app.setCode("APP-001");
        app.setName("测试应用");
        app.setDescription("描述");
        app.setState(ApplicationState.ACTIVE);
        app.setCreatedAt(Instant.parse("2026-06-19T10:00:00Z"));
        app.setUpdatedAt(Instant.parse("2026-06-19T10:00:00Z"));
        return app;
    }

    @Test
    @DisplayName("POST /api/v1/applications 创建成功返回 201 与响应体")
    void create_returns201WithBody() throws Exception {
        when(applicationService.create(any())).thenReturn(stubApp());

        ApplicationRequest request = new ApplicationRequest();
        request.setCode("APP-001");
        request.setName("测试应用");
        request.setDescription("描述");

        mockMvc.perform(post("/api/v1/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.code").value("APP-001"))
                .andExpect(jsonPath("$.state").value("ACTIVE"));
    }

    @Test
    @DisplayName("GET /api/v1/applications 返回应用列表")
    void list_returnsArray() throws Exception {
        when(applicationService.getAll()).thenReturn(List.of(stubApp()));

        mockMvc.perform(get("/api/v1/applications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].code").value("APP-001"));
    }

    @Test
    @DisplayName("GET /api/v1/applications/{id} 返回应用详情")
    void getById_returnsDetail() throws Exception {
        when(applicationService.getById(1L)).thenReturn(stubApp());

        mockMvc.perform(get("/api/v1/applications/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("测试应用"));
    }

    @Test
    @DisplayName("PUT /api/v1/applications/{id} 更新成功返回 200")
    void update_returns200() throws Exception {
        when(applicationService.update(eq(1L), any())).thenReturn(stubApp());

        ApplicationRequest request = new ApplicationRequest();
        request.setCode("APP-001");
        request.setName("新名称");

        mockMvc.perform(put("/api/v1/applications/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @DisplayName("DELETE /api/v1/applications/{id} 返回 204")
    void delete_returns204() throws Exception {
        mockMvc.perform(delete("/api/v1/applications/{id}", 1L))
                .andExpect(status().isNoContent());
        verify(applicationService).delete(1L);
    }

    @Test
    @DisplayName("GET /api/v1/applications/{id}/channels 返回渠道授权项列表（含 priority）")
    void listChannels_returnsChannelItems() throws Exception {
        ApplicationChannel rel1 = new ApplicationChannel(1L, 10L);
        rel1.setPriority(1);
        ApplicationChannel rel2 = new ApplicationChannel(1L, 20L);
        when(applicationService.listChannels(1L)).thenReturn(List.of(rel1, rel2));

        mockMvc.perform(get("/api/v1/applications/{id}/channels", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].channelId").value(10))
                .andExpect(jsonPath("$[0].priority").value(1))
                .andExpect(jsonPath("$[1].channelId").value(20))
                .andExpect(jsonPath("$[1].priority").doesNotExist());
    }

    @Test
    @DisplayName("PUT /api/v1/applications/{id}/channels 更新渠道授权（含 priority）返回 204")
    void updateChannels_returns204() throws Exception {
        mockMvc.perform(put("/api/v1/applications/{id}/channels", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"channels\":[{\"channelId\":10,\"priority\":1},{\"channelId\":20}]}"))
                .andExpect(status().isNoContent());
        verify(applicationService).updateChannels(eq(1L), argThat(items ->
                items != null && items.size() == 2
                        && items.get(0).getChannelId().equals(10L) && items.get(0).getPriority().equals(1)
                        && items.get(1).getChannelId().equals(20L) && items.get(1).getPriority() == null));
    }

    @Test
    @DisplayName("PUT /api/v1/applications/{id}/channels 空列表表示清空授权返回 204")
    void updateChannels_emptyList_returns204() throws Exception {
        mockMvc.perform(put("/api/v1/applications/{id}/channels", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"channels\":[]}"))
                .andExpect(status().isNoContent());
        verify(applicationService).updateChannels(eq(1L), eq(List.of()));
    }

    @Test
    @DisplayName("PUT /api/v1/applications/{id}/channels 含 null channelId 元素返回 400 且不调用 service")
    void updateChannels_nullChannelId_returns400() throws Exception {
        mockMvc.perform(put("/api/v1/applications/{id}/channels", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"channels\":[{\"channelId\":null}]}"))
                .andExpect(status().isBadRequest());
        verify(applicationService, never()).updateChannels(any(), any());
    }

    @Test
    @DisplayName("PUT /api/v1/applications/{id}/channels 含负数 channelId 返回 400 且不调用 service")
    void updateChannels_negativeChannelId_returns400() throws Exception {
        mockMvc.perform(put("/api/v1/applications/{id}/channels", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"channels\":[{\"channelId\":-1}]}"))
                .andExpect(status().isBadRequest());
        verify(applicationService, never()).updateChannels(any(), any());
    }

    @Test
    @DisplayName("PUT /api/v1/applications/{id}/channels 含 0 channelId 返回 400 且不调用 service")
    void updateChannels_zeroChannelId_returns400() throws Exception {
        mockMvc.perform(put("/api/v1/applications/{id}/channels", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"channels\":[{\"channelId\":0}]}"))
                .andExpect(status().isBadRequest());
        verify(applicationService, never()).updateChannels(any(), any());
    }

    @Test
    @DisplayName("PUT /api/v1/applications/{id}/channels channels 为 null 返回 400")
    void updateChannels_nullList_returns400() throws Exception {
        mockMvc.perform(put("/api/v1/applications/{id}/channels", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
        verify(applicationService, never()).updateChannels(any(), any());
    }

    @Test
    @DisplayName("POST /api/v1/applications 携带 timeout 创建成功返回 201")
    void create_withTimeout_returns201() throws Exception {
        when(applicationService.create(any())).thenReturn(stubApp());

        ApplicationRequest request = new ApplicationRequest();
        request.setCode("APP-001");
        request.setName("测试应用");
        request.setDescription("描述");
        request.setTimeout(60);

        mockMvc.perform(post("/api/v1/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));
        // Task 8：timeout 随 ApplicationRequest 透传至 service（端点契约）
        verify(applicationService).create(any());
    }
}
