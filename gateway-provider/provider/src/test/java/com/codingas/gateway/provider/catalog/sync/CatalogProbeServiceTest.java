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
package com.codingas.gateway.provider.catalog.sync;

import com.codingas.gateway.protocol.Protocol;
import com.codingas.gateway.provider.channel.Channel;
import com.codingas.gateway.provider.channel.ChannelCredential;
import com.codingas.gateway.provider.channel.ChannelCredentialService;
import com.codingas.gateway.provider.channel.ChannelEndpoint;
import com.codingas.gateway.provider.channel.ChannelService;
import com.codingas.gateway.provider.model.ModelDeprecationService;
import com.codingas.gateway.provider.model.ModelInstance;
import com.codingas.gateway.provider.model.ModelInstanceRepository;
import com.codingas.gateway.provider.model.ModelRepository;
import com.codingas.gateway.settings.SystemSettingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * CatalogProbeService 单元测试
 *
 * <p>验证上游列表探测编排：消失连续 N 次转 DEPRECATED、重新出现恢复 ACTIVE、
 * 无凭证/协议不支持渠道跳过、总开关关闭不执行。</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CatalogProbeService 列表探测编排")
class CatalogProbeServiceTest {

    @Mock private ChannelService channelService;
    @Mock private ChannelCredentialService credentialService;
    @Mock private ModelInstanceRepository instanceRepository;
    @Mock private ModelRepository modelRepository;
    @Mock private UpstreamModelProbeClient probeClient;
    @Mock private ModelDeprecationService deprecationService;
    @Mock private CatalogSyncLogRepository logRepository;
    @Mock private SystemSettingService settingService;
    @InjectMocks private CatalogProbeService probeService;

    @Test
    @DisplayName("上游消失连续 N 次后实例转 DEPRECATED")
    void missingConsecutively_marksInstanceDeprecated() {
        // given：渠道 1 有 endpoint(OPENAI)+凭证；实例 upstreamModelName="gpt-4"
        //      probeClient.fetchModelIds 返回空集合（不含 gpt-4）；confirm-count=2
        whenProbeEnabled();
        when(settingService.getInt("catalog.deprecation.confirm-count", 3)).thenReturn(2);
        Channel channel = channel(1L, "渠道A");
        when(channelService.getAll()).thenReturn(List.of(channel));
        when(channelService.getEndpoints(1L)).thenReturn(List.of(endpoint(Protocol.OPENAI)));
        when(credentialService.listByChannelId(1L)).thenReturn(List.of(credential("sk-1")));
        ModelInstance instance = instance(1L, 10L, "gpt-4", ModelInstance.State.ACTIVE);
        when(instanceRepository.findByChannelId(1L)).thenReturn(List.of(instance));
        when(probeClient.fetchModelIds(any(), any())).thenReturn(Set.of());

        // when：probe() 连续调用两次，上游持续缺失
        probeService.probe();
        probeService.probe();

        // then：第二次后计数达到阈值（2），标记 DEPRECATED
        verify(deprecationService).markInstanceDeprecated(1L);
    }

    @Test
    @DisplayName("上游重新出现则恢复 ACTIVE")
    void reappears_restoresInstance() {
        // given：confirm-count=1；实例 ACTIVE；第一次探测缺失 → 标记 DEPRECATED
        whenProbeEnabled();
        when(settingService.getInt("catalog.deprecation.confirm-count", 3)).thenReturn(1);
        Channel channel = channel(1L, "渠道A");
        when(channelService.getAll()).thenReturn(List.of(channel));
        when(channelService.getEndpoints(1L)).thenReturn(List.of(endpoint(Protocol.OPENAI)));
        when(credentialService.listByChannelId(1L)).thenReturn(List.of(credential("sk-1")));
        ModelInstance instance = instance(1L, 10L, "gpt-4", ModelInstance.State.ACTIVE);
        when(instanceRepository.findByChannelId(1L)).thenReturn(List.of(instance));
        when(probeClient.fetchModelIds(any(), any())).thenReturn(Set.of());

        // when：第一次探测缺失（计数 1 → 达到阈值）→ 实例被标记 DEPRECATED
        probeService.probe();
        verify(deprecationService).markInstanceDeprecated(1L);
        // 模拟 deprecationService 真实落地状态（mock 不会自动变更实例状态）
        instance.setState(ModelInstance.State.DEPRECATED);

        // 第二次探测：上游重新出现 → 恢复 ACTIVE
        when(probeClient.fetchModelIds(any(), any())).thenReturn(Set.of("gpt-4"));
        probeService.probe();

        // then
        verify(deprecationService).restoreInstance(1L);
    }

    @Test
    @DisplayName("无凭证或协议不支持的渠道跳过")
    void channelWithoutCredential_skipped() {
        // given：渠道 1 仅 NATIVE 端点（协议不支持列表 API）；渠道 2 有 OPENAI 端点但无凭证
        whenProbeEnabled();
        Channel nativeOnly = channel(1L, "原生渠道");
        Channel noCredential = channel(2L, "无凭证渠道");
        when(channelService.getAll()).thenReturn(List.of(nativeOnly, noCredential));
        when(channelService.getEndpoints(1L)).thenReturn(List.of(endpoint(Protocol.NATIVE)));
        when(channelService.getEndpoints(2L)).thenReturn(List.of(endpoint(Protocol.OPENAI)));
        when(credentialService.listByChannelId(2L)).thenReturn(List.of());

        // when
        probeService.probe();

        // then：两个渠道均被跳过，probeClient 不被调用
        verify(probeClient, never()).fetchModelIds(any(), any());
    }

    @Test
    @DisplayName("探测总开关关闭时不执行")
    void disabled_skips() {
        when(settingService.getBoolean("catalog.deprecation.enabled", true)).thenReturn(false);

        probeService.probe();

        verify(probeClient, never()).fetchModelIds(any(), any());
    }

    /** 打开探测总开关与探测子开关 */
    private void whenProbeEnabled() {
        when(settingService.getBoolean("catalog.deprecation.enabled", true)).thenReturn(true);
        when(settingService.getBoolean("catalog.deprecation.probe.enabled", true)).thenReturn(true);
    }

    /** 构造渠道测试对象 */
    private Channel channel(Long id, String name) {
        Channel channel = new Channel();
        channel.setId(id);
        channel.setName(name);
        return channel;
    }

    /** 构造渠道端点测试对象 */
    private ChannelEndpoint endpoint(Protocol protocol) {
        ChannelEndpoint endpoint = new ChannelEndpoint();
        endpoint.setProtocol(protocol);
        endpoint.setEndpointUrl("https://api.example.com");
        return endpoint;
    }

    /** 构造渠道凭证测试对象 */
    private ChannelCredential credential(String apiKey) {
        ChannelCredential credential = new ChannelCredential();
        credential.setApiKeyPlain(apiKey);
        return credential;
    }

    /** 构造模型实例测试对象 */
    private ModelInstance instance(Long id, Long modelId, String upstreamName, ModelInstance.State state) {
        ModelInstance instance = new ModelInstance();
        instance.setId(id);
        instance.setModelId(modelId);
        instance.setUpstreamModelName(upstreamName);
        instance.setState(state);
        return instance;
    }
}
