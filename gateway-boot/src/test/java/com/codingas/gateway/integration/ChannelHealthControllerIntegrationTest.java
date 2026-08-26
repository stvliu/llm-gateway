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
package com.codingas.gateway.integration;

import com.codingas.gateway.boot.GatewayApplication;
import com.codingas.gateway.provider.channel.ChannelEmergencyManager;
import com.codingas.gateway.provider.channel.ChannelManager;
import com.codingas.gateway.provider.channel.ChannelHealthManager;
import com.codingas.gateway.provider.upstream.KeyTestResult;
import com.codingas.gateway.provider.channel.Channel;
import com.codingas.gateway.provider.channel.ChannelCredential;
import com.codingas.gateway.provider.vendor.Provider;
import com.codingas.gateway.provider.model.BillingMode;
import com.codingas.gateway.provider.channel.ChannelHealthSource;
import com.codingas.gateway.provider.channel.ChannelHealthStatus;
import com.codingas.gateway.provider.channel.ChannelState;
import com.codingas.gateway.provider.channel.ChannelCredentialRepository;
import com.codingas.gateway.provider.channel.ChannelRepository;
import com.codingas.gateway.provider.channel.ChannelKeyProbe;
import com.codingas.gateway.provider.vendor.ProviderRepository;
import com.codingas.gateway.web.advice.GlobalExceptionHandler;
import com.codingas.gateway.web.api.ChannelController;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * POST /api/v1/channels/{id}/health-check 端到端集成测试。
 *
 * <p>覆盖：</p>
 * <ul>
 *   <li>DRAWER 来源：返回矩阵并写入持久化字段</li>
 *   <li>PRECHECK 来源：返回结果但不写入持久化</li>
 *   <li>零 Key 渠道：聚合为 UNKNOWN</li>
 *   <li>缺少 source 字段：返回 400</li>
 *   <li>不存在的 channelId：返回 4xx</li>
 *   <li>并发触发 last-write-wins（暂禁用，待 Awaitility 重写）</li>
 * </ul>
 */
@SpringBootTest(classes = GatewayApplication.class)
@ActiveProfiles("test")
@DisplayName("ChannelHealthController 集成测试")
class ChannelHealthControllerIntegrationTest {

    @Autowired
    private ChannelRepository channelRepository;

    @Autowired
    private ChannelCredentialRepository channelCredentialRepository;

    @Autowired
    private ProviderRepository providerRepository;

    @Autowired
    private ChannelHealthManager channelHealthManager;

    @Autowired
    private ChannelManager channelManager;

    @Autowired
    private ChannelEmergencyManager channelEmergencyManager;

    @Autowired
    private com.codingas.gateway.web.api.assembler.ChannelFacade channelFacade;

    @Autowired
    private GlobalExceptionHandler globalExceptionHandler;

    @MockBean
    private ChannelKeyProbe channelKeyProbe;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        // standaloneSetup 跳过 SecurityInterceptorChain 等业务拦截器，
        // 仅装配本次端点所需的 Controller + 全局异常处理
        ChannelController controller = new ChannelController(channelFacade, channelManager,
                channelHealthManager, channelEmergencyManager);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(globalExceptionHandler)
                .build();
    }

    /**
     * 准备一个完整的 Provider + Channel + 多条 Credential
     */
    private Long prepareChannel(int credentialCount) {
        Provider provider = new Provider();
        provider.setCode("health-it-provider-" + System.nanoTime());
        provider.setName("健康测试供应商");
        Provider savedProvider = providerRepository.save(provider);

        Channel channel = new Channel();
        channel.setProviderId(savedProvider.getId());
        channel.setName("health-it-channel-" + System.nanoTime());
        channel.setBillingMode(BillingMode.PAY_AS_YOU_GO);
        channel.setState(ChannelState.ACTIVE);
        Channel savedChannel = channelRepository.save(channel);

        for (int i = 0; i < credentialCount; i++) {
            ChannelCredential c = new ChannelCredential();
            c.setChannelId(savedChannel.getId());
            c.setName("key-" + i);
            c.setApiKeyPlain("sk-test-key-" + i + "-1234567890");
            c.setApiKeyPrefix("sk-test-");
            c.setKeyAlias("alias-" + i);
            c.setWeight(1);
            c.setPriority(1);
            channelCredentialRepository.save(c);
        }

        return savedChannel.getId();
    }

    /**
     * mock 探针返回全部 PASS 且各自含 1 个可用模型
     */
    private void stubAllPass() {
        when(channelKeyProbe.test(any(Channel.class), any(ChannelCredential.class)))
                .thenAnswer(inv -> {
                    ChannelCredential c = inv.getArgument(1);
                    return KeyTestResult.pass(c.getId(), c.getApiKeyPlain(), List.of("gpt-4"), 100L);
                });
    }

    @Test
    @DisplayName("DRAWER 来源应返回矩阵并写入持久化字段")
    void DRAWER_来源应返回矩阵并写入持久化字段() throws Exception {
        Long channelId = prepareChannel(2);
        stubAllPass();

        String body = objectMapper.writeValueAsString(Map.of("source", "DRAWER"));

        mockMvc.perform(post("/api/v1/channels/{id}/health-check", channelId)
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.aggregateStatus").value("HEALTHY"))
                .andExpect(jsonPath("$.matrix.length()").value(2))
                .andExpect(jsonPath("$.channelId").value(channelId));

        // 断言持久化已发生
        Channel reloaded = channelRepository.findById(channelId).orElseThrow();
        assertThat(reloaded.getLastHealthStatus()).isEqualTo(ChannelHealthStatus.HEALTHY);
        assertThat(reloaded.getLastHealthSource()).isEqualTo(ChannelHealthSource.DRAWER);
        assertThat(reloaded.getLastHealthCheckAt()).isNotNull();
    }

    @Test
    @DisplayName("PRECHECK 来源不写入持久化字段")
    void PRECHECK_来源不写入持久化字段() throws Exception {
        Long channelId = prepareChannel(1);
        stubAllPass();

        String body = objectMapper.writeValueAsString(Map.of("source", "PRECHECK"));

        mockMvc.perform(post("/api/v1/channels/{id}/health-check", channelId)
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.aggregateStatus").value("HEALTHY"));

        // 断言三字段保持 null
        Channel reloaded = channelRepository.findById(channelId).orElseThrow();
        assertThat(reloaded.getLastHealthStatus()).isNull();
        assertThat(reloaded.getLastHealthSource()).isNull();
        assertThat(reloaded.getLastHealthCheckAt()).isNull();
    }

    @Test
    @DisplayName("零 Key 渠道返回 UNKNOWN，矩阵为空")
    void 零_Key_返回_UNKNOWN() throws Exception {
        Long channelId = prepareChannel(0);

        String body = objectMapper.writeValueAsString(Map.of("source", "DRAWER"));

        mockMvc.perform(post("/api/v1/channels/{id}/health-check", channelId)
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.aggregateStatus").value("UNKNOWN"))
                .andExpect(jsonPath("$.matrix.length()").value(0));
    }

    @Test
    @DisplayName("缺少 source 字段返回 400")
    void 缺少_source_字段返回_400() throws Exception {
        Long channelId = prepareChannel(1);

        // 空对象，无 source
        mockMvc.perform(post("/api/v1/channels/{id}/health-check", channelId)
                        .contentType(APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("不存在的 channelId 返回 4xx")
    void 不存在的_channelId_返回_4xx() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("source", "DRAWER"));

        // 渠道不存在抛 GatewayRequestException → 由全局异常处理器映射为 4xx（具体 status 取决于映射）
        mockMvc.perform(post("/api/v1/channels/{id}/health-check", 999_999_999L)
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().is4xxClientError());
    }
}
