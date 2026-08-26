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
package com.codingas.gateway.provider.channel;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * ChannelHealthManager 单元测试
 *
 * <p>覆盖聚合规则四分支、PRECHECK 不持久化、持久化失败兜底、aggregate 静态方法多分支。</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ChannelHealthManager 单元测试")
class ChannelHealthManagerTest {

    @Mock
    private ChannelRepository channelRepository;
    @Mock
    private ChannelCredentialRepository credentialRepository;
    @Mock
    private ChannelKeyProbe channelKeyProbe;

    /** 同步 Executor 简化测试 */
    private final Executor executor = Runnable::run;

    private ChannelHealthManager service;

    private static final Long CHANNEL_ID = 100L;

    @BeforeEach
    void setUp() {
        service = new ChannelHealthManager(channelRepository, credentialRepository, channelKeyProbe, executor);
    }

    /**
     * 准备一个简单的 Channel 实体桩
     */
    private Channel stubChannel() {
        Channel channel = new Channel();
        channel.setId(CHANNEL_ID);
        channel.setName("test-channel");
        channel.setProviderId(1L);
        return channel;
    }

    /**
     * 准备一条 Credential 桩
     */
    private ChannelCredential stubCredential(Long id, String prefix) {
        ChannelCredential c = new ChannelCredential();
        c.setId(id);
        c.setChannelId(CHANNEL_ID);
        c.setApiKeyPrefix(prefix);
        return c;
    }

    @Test
    @DisplayName("全部 Key 通过且各有可用模型 → HEALTHY")
    void 全部_Key_通过聚合为_HEALTHY() {
        when(channelRepository.findById(CHANNEL_ID)).thenReturn(Optional.of(stubChannel()));
        ChannelCredential c1 = stubCredential(1L, "sk-aa");
        ChannelCredential c2 = stubCredential(2L, "sk-bb");
        when(credentialRepository.findByChannelId(CHANNEL_ID)).thenReturn(List.of(c1, c2));
        when(channelKeyProbe.test(any(Channel.class), eq(c1)))
                .thenReturn(KeyTestResult.pass(1L, "sk-aa", List.of("gpt-4"), 120L));
        when(channelKeyProbe.test(any(Channel.class), eq(c2)))
                .thenReturn(KeyTestResult.pass(2L, "sk-bb", List.of("claude-3"), 130L));

        ChannelHealthResult result = service.check(CHANNEL_ID, ChannelHealthSource.DRAWER);

        assertThat(result.aggregateStatus()).isEqualTo(ChannelHealthStatus.HEALTHY);
        assertThat(result.matrix()).hasSize(2);
        verify(channelRepository).save(any(Channel.class));
    }

    @Test
    @DisplayName("部分 Key 通过部分失败 → DEGRADED")
    void 部分通过聚合为_DEGRADED() {
        when(channelRepository.findById(CHANNEL_ID)).thenReturn(Optional.of(stubChannel()));
        ChannelCredential c1 = stubCredential(1L, "sk-aa");
        ChannelCredential c2 = stubCredential(2L, "sk-bb");
        when(credentialRepository.findByChannelId(CHANNEL_ID)).thenReturn(List.of(c1, c2));
        when(channelKeyProbe.test(any(Channel.class), eq(c1)))
                .thenReturn(KeyTestResult.pass(1L, "sk-aa", List.of("gpt-4"), 120L));
        when(channelKeyProbe.test(any(Channel.class), eq(c2)))
                .thenReturn(KeyTestResult.fail(2L, "sk-bb", "401 unauthorized"));

        ChannelHealthResult result = service.check(CHANNEL_ID, ChannelHealthSource.CARD);

        assertThat(result.aggregateStatus()).isEqualTo(ChannelHealthStatus.DEGRADED);
    }

    @Test
    @DisplayName("全部 Key 失败 → FAILED")
    void 全部失败聚合为_FAILED() {
        when(channelRepository.findById(CHANNEL_ID)).thenReturn(Optional.of(stubChannel()));
        ChannelCredential c1 = stubCredential(1L, "sk-aa");
        ChannelCredential c2 = stubCredential(2L, "sk-bb");
        when(credentialRepository.findByChannelId(CHANNEL_ID)).thenReturn(List.of(c1, c2));
        when(channelKeyProbe.test(any(Channel.class), eq(c1)))
                .thenReturn(KeyTestResult.fail(1L, "sk-aa", "boom"));
        when(channelKeyProbe.test(any(Channel.class), eq(c2)))
                .thenReturn(KeyTestResult.fail(2L, "sk-bb", "401"));

        ChannelHealthResult result = service.check(CHANNEL_ID, ChannelHealthSource.DRAWER);

        assertThat(result.aggregateStatus()).isEqualTo(ChannelHealthStatus.FAILED);
    }

    @Test
    @DisplayName("无 Key 列表 → UNKNOWN")
    void 无_Key_聚合为_UNKNOWN() {
        when(channelRepository.findById(CHANNEL_ID)).thenReturn(Optional.of(stubChannel()));
        when(credentialRepository.findByChannelId(CHANNEL_ID)).thenReturn(List.of());

        ChannelHealthResult result = service.check(CHANNEL_ID, ChannelHealthSource.DRAWER);

        assertThat(result.aggregateStatus()).isEqualTo(ChannelHealthStatus.UNKNOWN);
        assertThat(result.matrix()).isEmpty();
    }

    @Test
    @DisplayName("PRECHECK 来源不写入持久化字段")
    void PRECHECK_来源不写入持久化字段() {
        when(channelRepository.findById(CHANNEL_ID)).thenReturn(Optional.of(stubChannel()));
        ChannelCredential c1 = stubCredential(1L, "sk-aa");
        when(credentialRepository.findByChannelId(CHANNEL_ID)).thenReturn(List.of(c1));
        when(channelKeyProbe.test(any(Channel.class), eq(c1)))
                .thenReturn(KeyTestResult.pass(1L, "sk-aa", List.of("gpt-4"), 100L));

        ChannelHealthResult result = service.check(CHANNEL_ID, ChannelHealthSource.PRECHECK);

        assertThat(result.aggregateStatus()).isEqualTo(ChannelHealthStatus.HEALTHY);
        // PRECHECK 来源不调用 save
        verify(channelRepository, never()).save(any(Channel.class));
    }

    @Test
    @DisplayName("持久化失败时主流程仍返回结果，不抛出异常")
    void 持久化失败时主流程仍返回结果() {
        when(channelRepository.findById(CHANNEL_ID)).thenReturn(Optional.of(stubChannel()));
        ChannelCredential c1 = stubCredential(1L, "sk-aa");
        when(credentialRepository.findByChannelId(CHANNEL_ID)).thenReturn(List.of(c1));
        when(channelKeyProbe.test(any(Channel.class), eq(c1)))
                .thenReturn(KeyTestResult.pass(1L, "sk-aa", List.of("gpt-4"), 100L));
        when(channelRepository.save(any(Channel.class))).thenThrow(new RuntimeException("DB down"));

        ChannelHealthResult result = service.check(CHANNEL_ID, ChannelHealthSource.DRAWER);

        // 主流程不抛出
        assertThat(result.aggregateStatus()).isEqualTo(ChannelHealthStatus.HEALTHY);
    }

    @Test
    @DisplayName("不存在的 channelId 抛出领域异常")
    void 不存在的_channelId_抛出异常() {
        when(channelRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.check(999L, ChannelHealthSource.DRAWER))
                .isInstanceOf(com.codingas.gateway.common.exception.GatewayRequestException.class)
                .hasMessageContaining("渠道不存在");
    }

    @Test
    @DisplayName("aggregate 静态方法覆盖各分支")
    void aggregate_静态方法多分支() {
        // 空列表 → UNKNOWN
        assertThat(ChannelHealthManager.aggregate(List.of()))
                .isEqualTo(ChannelHealthStatus.UNKNOWN);

        // 全部 PASS 且有模型 → HEALTHY
        assertThat(ChannelHealthManager.aggregate(List.of(
                KeyTestResult.pass(1L, "sk-a", List.of("m1"), 10L),
                KeyTestResult.pass(2L, "sk-b", List.of("m2"), 20L)
        ))).isEqualTo(ChannelHealthStatus.HEALTHY);

        // 全部 FAIL → FAILED
        assertThat(ChannelHealthManager.aggregate(List.of(
                KeyTestResult.fail(1L, "sk-a", "err"),
                KeyTestResult.fail(2L, "sk-b", "err")
        ))).isEqualTo(ChannelHealthStatus.FAILED);

        // 部分 PASS 部分 FAIL → DEGRADED
        assertThat(ChannelHealthManager.aggregate(List.of(
                KeyTestResult.pass(1L, "sk-a", List.of("m1"), 10L),
                KeyTestResult.fail(2L, "sk-b", "err")
        ))).isEqualTo(ChannelHealthStatus.DEGRADED);

        // PASS 但模型列表为空 → 不计为通过 → FAILED
        assertThat(ChannelHealthManager.aggregate(List.of(
                KeyTestResult.pass(1L, "sk-a", List.of(), 10L)
        ))).isEqualTo(ChannelHealthStatus.FAILED);

        // TIMEOUT 算失败
        assertThat(ChannelHealthManager.aggregate(List.of(
                KeyTestResult.timeout(1L, "sk-a")
        ))).isEqualTo(ChannelHealthStatus.FAILED);
    }

    @Test
    @DisplayName("单 Key 探测抛异常 → exceptionally 兜底为 TIMEOUT，聚合为 FAILED")
    void 探测异常兜底为_TIMEOUT() {
        when(channelRepository.findById(CHANNEL_ID)).thenReturn(Optional.of(stubChannel()));
        ChannelCredential c1 = stubCredential(1L, "sk-aa");
        ChannelCredential c2 = stubCredential(2L, "sk-bb");
        c2.setApiKeyPlain("sk-plain-2222");
        when(credentialRepository.findByChannelId(CHANNEL_ID)).thenReturn(List.of(c1, c2));
        when(channelKeyProbe.test(any(Channel.class), eq(c1)))
                .thenThrow(new RuntimeException("probe crashed"));
        when(channelKeyProbe.test(any(Channel.class), eq(c2)))
                .thenThrow(new RuntimeException("probe crashed"));

        ChannelHealthResult result = service.check(CHANNEL_ID, ChannelHealthSource.DRAWER);

        // 两条 Key 均超时 → 全部失败 → FAILED
        assertThat(result.aggregateStatus()).isEqualTo(ChannelHealthStatus.FAILED);
        assertThat(result.matrix()).hasSize(2);
        assertThat(result.matrix()).allSatisfy(row -> assertThat(row.auth()).isEqualTo(AuthStatus.TIMEOUT));
    }

    @Test
    @DisplayName("maskKey 脱敏分支：null/空白/短 Key/长 Key")
    void maskKey_脱敏分支() {
        // null / 空白 → 固定 ***
        assertThat(ChannelHealthManager.maskKey(null)).isEqualTo("***");
        assertThat(ChannelHealthManager.maskKey("   ")).isEqualTo("***");
        // 长度 <= 8 → 固定 ***
        assertThat(ChannelHealthManager.maskKey("sk-test")).isEqualTo("***");
        // 长 Key → 前缀4 + ***... + 后缀4
        assertThat(ChannelHealthManager.maskKey("sk-test-key-123456789"))
                .isEqualTo("sk-t***...6789");
    }

    @Test
    @DisplayName("KeyTestResult 工厂方法状态字段正确")
    void KeyTestResult_工厂方法() {
        KeyTestResult pass = KeyTestResult.pass(1L, "sk-a", List.of("m1"), 100L);
        assertThat(pass.auth()).isEqualTo(AuthStatus.PASS);
        assertThat(pass.availableModels()).containsExactly("m1");
        assertThat(pass.latencyMs()).isEqualTo(100L);

        KeyTestResult fail = KeyTestResult.fail(2L, "sk-b", "401");
        assertThat(fail.auth()).isEqualTo(AuthStatus.FAIL);
        assertThat(fail.errorMessage()).isEqualTo("401");

        KeyTestResult timeout = KeyTestResult.timeout(3L, "sk-c");
        assertThat(timeout.auth()).isEqualTo(AuthStatus.TIMEOUT);
        assertThat(timeout.availableModels()).isEmpty();
    }
}
