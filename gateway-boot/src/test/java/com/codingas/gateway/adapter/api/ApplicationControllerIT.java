package com.codingas.gateway.adapter.api;

import com.codingas.gateway.application.application.ApplicationService;
import com.codingas.gateway.application.application.dto.ApplicationRequest;
import com.codingas.gateway.application.application.dto.ApplicationResponse;
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
class ApplicationControllerIT {

    @Mock
    private ApplicationService applicationService;

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        ApplicationController controller = new ApplicationController(applicationService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    /**
     * 构造含完整字段的应用响应桩
     */
    private ApplicationResponse stubResponse() {
        ApplicationResponse resp = new ApplicationResponse();
        resp.setId(1L);
        resp.setCode("APP-001");
        resp.setName("测试应用");
        resp.setDescription("描述");
        resp.setState("ACTIVE");
        resp.setCreatedAt(Instant.parse("2026-06-19T10:00:00Z"));
        resp.setUpdatedAt(Instant.parse("2026-06-19T10:00:00Z"));
        return resp;
    }

    @Test
    @DisplayName("POST /api/v1/applications 创建成功返回 201 与响应体")
    void create_returns201WithBody() throws Exception {
        when(applicationService.create(any())).thenReturn(stubResponse());

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
        when(applicationService.getAll()).thenReturn(List.of(stubResponse()));

        mockMvc.perform(get("/api/v1/applications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].code").value("APP-001"));
    }

    @Test
    @DisplayName("GET /api/v1/applications/{id} 返回应用详情")
    void getById_returnsDetail() throws Exception {
        when(applicationService.getById(1L)).thenReturn(stubResponse());

        mockMvc.perform(get("/api/v1/applications/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("测试应用"));
    }

    @Test
    @DisplayName("PUT /api/v1/applications/{id} 更新成功返回 200")
    void update_returns200() throws Exception {
        when(applicationService.update(eq(1L), any())).thenReturn(stubResponse());

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
    @DisplayName("GET /api/v1/applications/{id}/channels 返回渠道 ID 列表")
    void listChannels_returnsChannelIds() throws Exception {
        when(applicationService.listChannelIds(1L)).thenReturn(List.of(10L, 20L));

        mockMvc.perform(get("/api/v1/applications/{id}/channels", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value(10))
                .andExpect(jsonPath("$[1]").value(20));
    }

    @Test
    @DisplayName("PUT /api/v1/applications/{id}/channels 更新渠道授权返回 204")
    void updateChannels_returns204() throws Exception {
        mockMvc.perform(put("/api/v1/applications/{id}/channels", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"channelIds\":[10,20]}"))
                .andExpect(status().isNoContent());
        verify(applicationService).updateChannels(eq(1L), eq(List.of(10L, 20L)));
    }
}
