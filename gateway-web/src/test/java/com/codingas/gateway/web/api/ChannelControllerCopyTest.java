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

import com.codingas.gateway.provider.channel.ChannelEmergencyService;
import com.codingas.gateway.provider.channel.ChannelHealthService;
import com.codingas.gateway.provider.channel.ChannelService;
import com.codingas.gateway.web.api.dto.ChannelCopyRequest;
import com.codingas.gateway.web.api.dto.ChannelResponse;
import com.codingas.gateway.web.api.facade.ChannelFacade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ChannelController 复制端点契约测试
 *
 * <p>验证 POST /api/v1/channels/{id}/copy 的请求解析与 copyCredentials 透传，
 * 响应组装由 {@link ChannelFacade} 负责（本测试以 mock 桩返回）。</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ChannelController 复制端点")
class ChannelControllerCopyTest {

    @Mock
    private ChannelFacade channelFacade;

    @Mock
    private ChannelService channelService;

    @Mock
    private ChannelHealthService channelHealthService;

    @Mock
    private ChannelEmergencyService channelEmergencyService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ChannelController controller = new ChannelController(channelFacade, channelService,
                channelHealthService, channelEmergencyService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("POST /api/v1/channels/{id}/copy 返回新渠道，copyCredentials 默认 false")
    void copy_returnsNewChannel_defaultNoCredentials() throws Exception {
        ChannelResponse resp = new ChannelResponse();
        resp.setId(2L);
        resp.setProviderId(10L);
        resp.setName("new-ch");
        resp.setBillingMode("PAY_AS_YOU_GO");
        resp.setState("ACTIVE");
        resp.setEndpoints(List.of());
        when(channelFacade.copy(eq(1L), any(ChannelCopyRequest.class))).thenReturn(resp);

        mockMvc.perform(post("/api/v1/channels/{id}/copy", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"new-ch\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.name").value("new-ch"));

        ArgumentCaptor<ChannelCopyRequest> captor = ArgumentCaptor.forClass(ChannelCopyRequest.class);
        verify(channelFacade).copy(eq(1L), captor.capture());
        assertThat(captor.getValue().isCopyCredentials()).isFalse();
        assertThat(captor.getValue().getName()).isEqualTo("new-ch");
    }

    @Test
    @DisplayName("copyCredentials=true 透传给门面")
    void copy_withCredentials_true() throws Exception {
        ChannelResponse resp = new ChannelResponse();
        resp.setId(3L);
        resp.setProviderId(10L);
        resp.setName("new-ch");
        when(channelFacade.copy(eq(1L), any(ChannelCopyRequest.class))).thenReturn(resp);

        mockMvc.perform(post("/api/v1/channels/{id}/copy", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"new-ch\",\"copyCredentials\":true}"))
                .andExpect(status().isOk());

        ArgumentCaptor<ChannelCopyRequest> captor = ArgumentCaptor.forClass(ChannelCopyRequest.class);
        verify(channelFacade).copy(eq(1L), captor.capture());
        assertThat(captor.getValue().isCopyCredentials()).isTrue();
    }

    @Test
    @DisplayName("name 缺失返回 400")
    void copy_missingName_badRequest() throws Exception {
        mockMvc.perform(post("/api/v1/channels/{id}/copy", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }
}
