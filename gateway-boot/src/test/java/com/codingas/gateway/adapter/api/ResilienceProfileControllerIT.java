package com.codingas.gateway.adapter.api;

import com.codingas.gateway.adapter.advice.GlobalExceptionHandler;
import com.codingas.gateway.application.resilience.ResilienceProfileService;
import com.codingas.gateway.application.resilience.dto.ResilienceProfileRequest;
import com.codingas.gateway.application.resilience.dto.ResilienceProfileResponse;
import com.codingas.gateway.common.exception.GatewayRequestException;
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
 * ResilienceProfileController HTTP 契约测试
 *
 * <p>使用 standalone setup + Mock {@link ResilienceProfileService}，验证 REST 端点路由、
 * HTTP 状态码与响应序列化契约，不连数据库。</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ResilienceProfileController 端点契约")
class ResilienceProfileControllerIT {

    @Mock
    private ResilienceProfileService resilienceProfileService;

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        ResilienceProfileController controller = new ResilienceProfileController(resilienceProfileService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    /**
     * 构造含完整字段的容灾画像响应桩
     */
    private ResilienceProfileResponse stubResponse() {
        ResilienceProfileResponse resp = new ResilienceProfileResponse();
        resp.setId(1L);
        resp.setCode("default");
        resp.setName("默认画像");
        resp.setMode("STANDARD");
        resp.setEnableL2ModelDegradation(true);
        resp.setDegradationMaxDepth(2);
        resp.setEnableSessionAffinity(false);
        resp.setSessionAffinityTtlMinutes(30);
        resp.setEnablePinnedModel(false);
        resp.setTimeout(0);
        resp.setCreatedAt(Instant.parse("2026-06-19T10:00:00Z"));
        resp.setUpdatedAt(Instant.parse("2026-06-19T10:00:00Z"));
        return resp;
    }

    private ResilienceProfileRequest buildRequest(String code, String name, String mode) {
        ResilienceProfileRequest request = new ResilienceProfileRequest();
        request.setCode(code);
        request.setName(name);
        request.setMode(mode);
        request.setEnableL2ModelDegradation(true);
        request.setDegradationMaxDepth(2);
        return request;
    }

    @Test
    @DisplayName("POST /api/v1/resilience/profiles 创建成功返回 201 与响应体")
    void create_returns201WithBody() throws Exception {
        when(resilienceProfileService.create(any())).thenReturn(stubResponse());

        ResilienceProfileRequest request = buildRequest("default", "默认画像", "STANDARD");

        mockMvc.perform(post("/api/v1/resilience/profiles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.code").value("default"))
                .andExpect(jsonPath("$.mode").value("STANDARD"));
    }

    @Test
    @DisplayName("POST /api/v1/resilience/profiles code 为空返回 400")
    void create_blankCode_returns400() throws Exception {
        ResilienceProfileRequest request = buildRequest("", "默认画像", "STANDARD");

        mockMvc.perform(post("/api/v1/resilience/profiles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
        verify(resilienceProfileService, never()).create(any());
    }

    @Test
    @DisplayName("POST /api/v1/resilience/profiles 非法 mode 返回 400 而非 500")
    void create_invalidMode_returns400() throws Exception {
        // Service 层将非法 mode 包装为 GatewayRequestException，
        // GlobalExceptionHandler.handleGatewayRequestException 应映射为 400（而非 500）
        ResilienceProfileRequest request = buildRequest("p1", "画像", "FOO");
        when(resilienceProfileService.create(any()))
                .thenThrow(new GatewayRequestException("RESILIENCE_MODE_INVALID",
                        "非法容灾模式: FOO，合法值: [STANDARD, STRICT, AGGRESSIVE]"));

        mockMvc.perform(post("/api/v1/resilience/profiles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("RESILIENCE_MODE_INVALID"));
    }

    @Test
    @DisplayName("GET /api/v1/resilience/profiles 返回画像列表")
    void list_returnsArray() throws Exception {
        when(resilienceProfileService.getAll()).thenReturn(List.of(stubResponse()));

        mockMvc.perform(get("/api/v1/resilience/profiles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].code").value("default"));
    }

    @Test
    @DisplayName("GET /api/v1/resilience/profiles/{id} 返回画像详情")
    void getById_returnsDetail() throws Exception {
        when(resilienceProfileService.getById(1L)).thenReturn(stubResponse());

        mockMvc.perform(get("/api/v1/resilience/profiles/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("默认画像"));
    }

    @Test
    @DisplayName("PUT /api/v1/resilience/profiles/{id} 更新成功返回 200")
    void update_returns200() throws Exception {
        when(resilienceProfileService.update(eq(1L), any())).thenReturn(stubResponse());

        ResilienceProfileRequest request = buildRequest("default", "新名称", "STANDARD");

        mockMvc.perform(put("/api/v1/resilience/profiles/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @DisplayName("PUT /api/v1/resilience/profiles/{id} mode 为空返回 400")
    void update_blankMode_returns400() throws Exception {
        ResilienceProfileRequest request = buildRequest("default", "名称", "");

        mockMvc.perform(put("/api/v1/resilience/profiles/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
        verify(resilienceProfileService, never()).update(any(), any());
    }
}
