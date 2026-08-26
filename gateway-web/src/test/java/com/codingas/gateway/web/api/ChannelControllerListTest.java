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

import com.codingas.gateway.provider.channel.Channel;
import com.codingas.gateway.provider.channel.ChannelEmergencyService;
import com.codingas.gateway.provider.channel.ChannelHealthService;
import com.codingas.gateway.provider.channel.ChannelHealthSource;
import com.codingas.gateway.provider.channel.ChannelHealthStatus;
import com.codingas.gateway.provider.channel.ChannelService;
import com.codingas.gateway.provider.channel.ChannelState;
import com.codingas.gateway.provider.channel.ChannelView;
import com.codingas.gateway.provider.model.BillingMode;
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
import java.util.Optional;

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
class ChannelControllerListTest {

    @Mock
    private ChannelService channelService;

    @Mock
    private ChannelHealthService channelHealthService;

    @Mock
    private ChannelEmergencyService channelEmergencyService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ChannelController controller = new ChannelController(channelService, channelHealthService, channelEmergencyService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    /**
     * 构造一个含完整健康字段的 Channel 视图桩（providerName/endpoints 已由核心组装）
     */
    private ChannelView stubChannelWithHealth() {
        Channel channel = new Channel();
        channel.setId(1L);
        channel.setProviderId(10L);
        channel.setName("ch-1");
        channel.setBillingMode(BillingMode.PAY_AS_YOU_GO);
        channel.setState(ChannelState.ACTIVE);
        channel.setLastHealthCheckAt(Instant.parse("2026-06-13T10:00:00Z"));
        channel.setLastHealthStatus(ChannelHealthStatus.HEALTHY);
        channel.setLastHealthSource(ChannelHealthSource.DRAWER);
        return new ChannelView(channel, "OpenAI", List.of());
    }

    @Test
    @DisplayName("GET /api/v1/channels 响应应包含三个健康字段")
    void getChannels_responseContainsHealthFields() throws Exception {
        when(channelService.getAll()).thenReturn(List.of(stubChannelWithHealth()));

        mockMvc.perform(get("/api/v1/channels"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].lastHealthCheckAt").exists())
                .andExpect(jsonPath("$[0].lastHealthStatus").value("HEALTHY"))
                .andExpect(jsonPath("$[0].lastHealthSource").value("DRAWER"));
    }

    @Test
    @DisplayName("GET /api/v1/channels/{id} 响应应包含三个健康字段")
    void getChannelById_responseContainsHealthFields() throws Exception {
        when(channelService.getById(1L)).thenReturn(stubChannelWithHealth());

        mockMvc.perform(get("/api/v1/channels/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lastHealthCheckAt").exists())
                .andExpect(jsonPath("$.lastHealthStatus").value("HEALTHY"))
                .andExpect(jsonPath("$.lastHealthSource").value("DRAWER"));
    }

    @Test
    @DisplayName("健康字段未赋值时序列化为 null（向后兼容，旧字段一个不删）")
    void healthFieldsNull_stillValidResponse() throws Exception {
        Channel channel = new Channel();
        channel.setId(2L);
        channel.setName("ch-2");
        channel.setProviderId(10L);
        channel.setBillingMode(BillingMode.PAY_AS_YOU_GO);
        channel.setState(ChannelState.PENDING);
        // 三个健康字段保持 null
        when(channelService.getById(2L)).thenReturn(new ChannelView(channel, null, List.of()));

        mockMvc.perform(get("/api/v1/channels/{id}", 2L))
                .andExpect(status().isOk())
                // 旧字段仍然存在（向后兼容硬约束）
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.name").value("ch-2"))
                .andExpect(jsonPath("$.state").value("PENDING"));
    }
}
