package com.codingas.gateway.adapter.api;

import com.codingas.gateway.adapter.advice.GlobalExceptionHandler;
import com.codingas.gateway.application.channel.ChannelEmergencyService;
import com.codingas.gateway.application.channel.ChannelService;
import com.codingas.gateway.application.supply.ChannelHealthService;
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

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * ChannelController 应急操作端点契约测试
 *
 * <p>使用 standalone setup + Mock 各 Service，验证一键熔断/恢复/状态查询/紧切域
 * 端点的路由、HTTP 状态码与响应契约，不连数据库。</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ChannelController 应急操作端点契约")
class ChannelEmergencyControllerIT {

    @Mock
    private ChannelService channelService;

    @Mock
    private ChannelHealthService channelHealthService;

    @Mock
    private ChannelEmergencyService channelEmergencyService;

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        ChannelController controller = new ChannelController(channelService, channelHealthService, channelEmergencyService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /channels/{cid}/endpoints/{eid}/circuit-breaker/force-open 一键熔断返回 200 与 OPEN")
    void forceOpen_returns200WithOpen() throws Exception {
        when(channelEmergencyService.forceOpen(eq(1L), eq(10L))).thenReturn("OPEN");

        mockMvc.perform(post("/api/v1/channels/1/endpoints/10/circuit-breaker/force-open"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("OPEN"));
        verify(channelEmergencyService).forceOpen(1L, 10L);
    }

    @Test
    @DisplayName("POST /channels/{cid}/endpoints/{eid}/circuit-breaker/force-close 一键恢复返回 200 与 CLOSED")
    void forceClose_returns200WithClosed() throws Exception {
        when(channelEmergencyService.forceClose(eq(1L), eq(10L))).thenReturn("CLOSED");

        mockMvc.perform(post("/api/v1/channels/1/endpoints/10/circuit-breaker/force-close"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("CLOSED"));
        verify(channelEmergencyService).forceClose(1L, 10L);
    }

    @Test
    @DisplayName("GET /channels/{cid}/endpoints/{eid}/circuit-breaker/state 查询状态返回 200")
    void getState_returns200WithState() throws Exception {
        when(channelEmergencyService.getState(eq(1L), eq(10L))).thenReturn("HALF_OPEN");

        mockMvc.perform(get("/api/v1/channels/1/endpoints/10/circuit-breaker/state"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("HALF_OPEN"));
    }

    @Test
    @DisplayName("PUT /channels/{id}/cluster 紧切域返回 204")
    void switchCluster_returns204() throws Exception {
        mockMvc.perform(put("/api/v1/channels/1/cluster")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clusterId\":200}"))
                .andExpect(status().isNoContent());
        verify(channelEmergencyService).switchCluster(1L, 200L);
    }

    @Test
    @DisplayName("PUT /channels/{id}/cluster clusterId 为空返回 400")
    void switchCluster_nullClusterId_returns400() throws Exception {
        mockMvc.perform(put("/api/v1/channels/1/cluster")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
        verify(channelEmergencyService, never()).switchCluster(any(), any());
    }
}
