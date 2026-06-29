package com.codingas.gateway.adapter.api;

import com.codingas.gateway.adapter.advice.GlobalExceptionHandler;
import com.codingas.gateway.application.resilience.ClusterService;
import com.codingas.gateway.application.resilience.dto.ClusterRequest;
import com.codingas.gateway.application.resilience.dto.ClusterResponse;
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
 * ClusterController HTTP 契约测试
 *
 * <p>使用 standalone setup + Mock {@link ClusterService}，验证 REST 端点路由、
 * HTTP 状态码与响应序列化契约，不连数据库。</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ClusterController 端点契约")
class ClusterControllerIT {

    @Mock
    private ClusterService clusterService;

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        ClusterController controller = new ClusterController(clusterService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    /**
     * 构造含完整字段的故障域响应桩
     */
    private ClusterResponse stubResponse() {
        ClusterResponse resp = new ClusterResponse();
        resp.setId(1L);
        resp.setCode("openai-us");
        resp.setName("OpenAI 美东");
        resp.setProviderId(10L);
        resp.setRegion("us-east");
        resp.setPriority(1);
        resp.setHealthStatus("HEALTHY");
        resp.setCreatedAt(Instant.parse("2026-06-19T10:00:00Z"));
        resp.setUpdatedAt(Instant.parse("2026-06-19T10:00:00Z"));
        return resp;
    }

    private ClusterRequest buildRequest(String code, String name, Long providerId) {
        ClusterRequest request = new ClusterRequest();
        request.setCode(code);
        request.setName(name);
        request.setProviderId(providerId);
        request.setRegion("us-east");
        request.setPriority(1);
        return request;
    }

    @Test
    @DisplayName("POST /api/v1/resilience/clusters 创建成功返回 201 与响应体")
    void create_returns201WithBody() throws Exception {
        when(clusterService.create(any())).thenReturn(stubResponse());

        ClusterRequest request = buildRequest("openai-us", "OpenAI 美东", 10L);

        mockMvc.perform(post("/api/v1/resilience/clusters")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.code").value("openai-us"))
                .andExpect(jsonPath("$.healthStatus").value("HEALTHY"));
    }

    @Test
    @DisplayName("POST /api/v1/resilience/clusters providerId 为空返回 400")
    void create_nullProviderId_returns400() throws Exception {
        ClusterRequest request = buildRequest("openai-us", "名称", null);

        mockMvc.perform(post("/api/v1/resilience/clusters")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
        verify(clusterService, never()).create(any());
    }

    @Test
    @DisplayName("GET /api/v1/resilience/clusters 返回故障域列表")
    void list_returnsArray() throws Exception {
        when(clusterService.getAll()).thenReturn(List.of(stubResponse()));

        mockMvc.perform(get("/api/v1/resilience/clusters"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].code").value("openai-us"));
    }

    @Test
    @DisplayName("GET /api/v1/resilience/clusters/{id} 返回故障域详情")
    void getById_returnsDetail() throws Exception {
        when(clusterService.getById(1L)).thenReturn(stubResponse());

        mockMvc.perform(get("/api/v1/resilience/clusters/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("OpenAI 美东"));
    }

    @Test
    @DisplayName("PUT /api/v1/resilience/clusters/{id} 更新成功返回 200")
    void update_returns200() throws Exception {
        when(clusterService.update(eq(1L), any())).thenReturn(stubResponse());

        ClusterRequest request = buildRequest("openai-us", "新名称", 10L);

        mockMvc.perform(put("/api/v1/resilience/clusters/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @DisplayName("PUT /api/v1/resilience/clusters/{id} code 为空返回 400")
    void update_blankCode_returns400() throws Exception {
        ClusterRequest request = buildRequest("", "名称", 10L);

        mockMvc.perform(put("/api/v1/resilience/clusters/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
        verify(clusterService, never()).update(any(), any());
    }
}
