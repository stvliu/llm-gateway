package com.codingas.gateway.adapter.api;

import com.codingas.gateway.application.channel.ChannelService;
import com.codingas.gateway.application.channel.dto.ChannelResponse;
import com.codingas.gateway.application.supply.ChannelHealthService;
import com.codingas.gateway.domain.supply.enums.ChannelHealthSource;
import com.codingas.gateway.domain.supply.enums.ChannelHealthStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * ChannelController 列表/详情响应字段的契约测试。
 *
 * <p>验证 GET /api/v1/channels 与 GET /api/v1/channels/{id} 响应中
 * lastHealthCheckAt / lastHealthStatus / lastHealthSource 三个字段存在。</p>
 *
 * <p>使用 standalone setup + Mock ChannelService，仅校验序列化契约，不连数据库。</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ChannelController 列表/详情响应字段")
class ChannelControllerListIT {

    @Mock
    private ChannelService channelService;

    @Mock
    private ChannelHealthService channelHealthService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ChannelController controller = new ChannelController(channelService, channelHealthService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    /**
     * 构造一个含完整健康字段的 ChannelResponse 桩
     */
    private ChannelResponse stubResponseWithHealth() {
        ChannelResponse resp = new ChannelResponse();
        resp.setId(1L);
        resp.setProviderId(10L);
        resp.setName("ch-1");
        resp.setBillingMode("PAY_AS_YOU_GO");
        resp.setState("ACTIVE");
        resp.setEndpoints(List.of());
        resp.setLastHealthCheckAt(Instant.parse("2026-06-13T10:00:00Z"));
        resp.setLastHealthStatus(ChannelHealthStatus.HEALTHY);
        resp.setLastHealthSource(ChannelHealthSource.DRAWER);
        return resp;
    }

    @Test
    @DisplayName("GET /api/v1/channels 响应应包含三个健康字段")
    void GET_channels_响应包含三个健康字段() throws Exception {
        when(channelService.getAll()).thenReturn(List.of(stubResponseWithHealth()));

        mockMvc.perform(get("/api/v1/channels"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].lastHealthCheckAt").exists())
                .andExpect(jsonPath("$[0].lastHealthStatus").value("HEALTHY"))
                .andExpect(jsonPath("$[0].lastHealthSource").value("DRAWER"));
    }

    @Test
    @DisplayName("GET /api/v1/channels/{id} 响应应包含三个健康字段")
    void GET_channel_详情包含三个健康字段() throws Exception {
        when(channelService.getById(1L)).thenReturn(stubResponseWithHealth());

        mockMvc.perform(get("/api/v1/channels/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lastHealthCheckAt").exists())
                .andExpect(jsonPath("$.lastHealthStatus").value("HEALTHY"))
                .andExpect(jsonPath("$.lastHealthSource").value("DRAWER"));
    }

    @Test
    @DisplayName("健康字段未赋值时序列化为 null（向后兼容，旧字段一个不删）")
    void 健康字段为_null_时仍是合法响应() throws Exception {
        ChannelResponse resp = new ChannelResponse();
        resp.setId(2L);
        resp.setName("ch-2");
        resp.setProviderId(10L);
        resp.setBillingMode("PAY_AS_YOU_GO");
        resp.setState("PENDING");
        resp.setEndpoints(List.of());
        // 三个健康字段保持 null
        when(channelService.getById(2L)).thenReturn(resp);

        mockMvc.perform(get("/api/v1/channels/{id}", 2L))
                .andExpect(status().isOk())
                // 旧字段仍然存在（向后兼容硬约束）
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.name").value("ch-2"))
                .andExpect(jsonPath("$.state").value("PENDING"));
    }
}
