package com.codingas.gateway.adapter.api;

import com.codingas.gateway.adapter.advice.GlobalExceptionHandler;
import com.codingas.gateway.application.resilience.ResilienceEventService;
import com.codingas.gateway.application.resilience.dto.FailoverEventResponse;
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

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ResilienceEventController HTTP 契约测试
 *
 * <p>使用 standalone setup + Mock {@link ResilienceEventService}，验证转移事件流查询端点路由、
 * HTTP 状态码与响应序列化契约，不连数据库。参照 {@link ResilienceProfileControllerIT} 范式。</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ResilienceEventController 端点契约")
class ResilienceEventControllerIT {

    @Mock
    private ResilienceEventService resilienceEventService;

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        ResilienceEventController controller = new ResilienceEventController(resilienceEventService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    /** 构造含完整字段的转移事件响应桩 */
    private FailoverEventResponse stubResponse() {
        FailoverEventResponse resp = new FailoverEventResponse();
        resp.setId(1L);
        resp.setTraceId("trace-abc-123");
        resp.setApplicationId(7L);
        resp.setFromChannelId(10L);
        resp.setFromEndpointId(20L);
        resp.setToChannelId(11L);
        resp.setToEndpointId(21L);
        resp.setFromClusterId(null);
        resp.setToClusterId(null);
        resp.setErrorType("AUTHENTICATION_ERROR");
        resp.setDecision("L1");
        resp.setExhausted(false);
        resp.setOccurredAt(Instant.parse("2026-06-22T10:00:00Z"));
        return resp;
    }

    @Test
    @DisplayName("GET /api/v1/resilience/events 默认参数返回事件列表")
    void list_defaultParams_returns200WithArray() throws Exception {
        when(resilienceEventService.findRecent(eq(null), eq(null), eq(null), eq(100)))
                .thenReturn(List.of(stubResponse()));

        mockMvc.perform(get("/api/v1/resilience/events")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].traceId").value("trace-abc-123"))
                .andExpect(jsonPath("$[0].fromChannelId").value(10))
                .andExpect(jsonPath("$[0].toChannelId").value(11))
                .andExpect(jsonPath("$[0].errorType").value("AUTHENTICATION_ERROR"))
                .andExpect(jsonPath("$[0].decision").value("L1"))
                .andExpect(jsonPath("$[0].exhausted").value(false));
    }

    @Test
    @DisplayName("GET /api/v1/resilience/events 携带 since/applicationId/clusterId/limit 过滤参数")
    void list_withFilters_delegatesToService() throws Exception {
        Instant since = Instant.parse("2026-06-22T00:00:00Z");
        when(resilienceEventService.findRecent(eq(since), eq(7L), eq(10L), eq(50)))
                .thenReturn(List.of(stubResponse()));

        mockMvc.perform(get("/api/v1/resilience/events")
                        .param("since", since.toString())
                        .param("applicationId", "7")
                        .param("clusterId", "10")
                        .param("limit", "50")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    @DisplayName("GET /api/v1/resilience/events limit 超上限 500 被截断为 500")
    void list_limitOverCap_cappedTo500() throws Exception {
        when(resilienceEventService.findRecent(eq(null), eq(null), eq(null), eq(500)))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/v1/resilience/events")
                        .param("limit", "9999")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/v1/resilience/events/exhausted 返回耗尽告警事件")
    void exhausted_returns200WithExhaustedEvents() throws Exception {
        FailoverEventResponse resp = stubResponse();
        resp.setExhausted(true);
        resp.setToChannelId(null);
        resp.setToEndpointId(null);
        when(resilienceEventService.findExhausted(eq(null), eq(50)))
                .thenReturn(List.of(resp));

        mockMvc.perform(get("/api/v1/resilience/events/exhausted")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].exhausted").value(true))
                .andExpect(jsonPath("$[0].toChannelId").doesNotExist());
    }

    @Test
    @DisplayName("GET /api/v1/resilience/events/exhausted 携带 since 和 limit 参数")
    void exhausted_withParams_delegatesToService() throws Exception {
        Instant since = Instant.parse("2026-06-22T00:00:00Z");
        when(resilienceEventService.findExhausted(eq(since), eq(30)))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/v1/resilience/events/exhausted")
                        .param("since", since.toString())
                        .param("limit", "30")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }
}
