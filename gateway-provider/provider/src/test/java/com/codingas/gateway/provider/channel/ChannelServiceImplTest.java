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

import com.codingas.gateway.common.exception.GatewayRequestException;
import com.codingas.gateway.provider.model.BillingMode;
import com.codingas.gateway.provider.model.ModelInstanceRepository;
import com.codingas.gateway.provider.upstream.Protocol;
import com.codingas.gateway.provider.vendor.Provider;
import com.codingas.gateway.provider.vendor.ProviderRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * ChannelServiceImpl 单元测试
 *
 * <p>验证 CRUD、端点管理与 toResponse 透传。状态转换（setState）分支由
 * {@code ChannelServiceImplStateTransitionTest} 覆盖。</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ChannelServiceImpl 测试")
class ChannelServiceImplTest {

    @Mock
    private ChannelRepository channelRepository;

    @Mock
    private ChannelEndpointRepository channelEndpointRepository;

    @Mock
    private ChannelCredentialRepository channelCredentialRepository;

    @Mock
    private ModelInstanceRepository modelInstanceRepository;

    @Mock
    private ProviderRepository providerRepository;

    @InjectMocks
    private ChannelServiceImpl channelService;

    @BeforeEach
    void setUp() {
        // toView 组装需要查询提供商名称与端点列表；lenient 避免未使用该查询的测试报 UnnecessaryStubbing
        lenient().when(providerRepository.findById(any())).thenReturn(Optional.empty());
        lenient().when(channelEndpointRepository.findByChannelId(any())).thenReturn(List.of());
    }

    // ==================== getById 测试 ====================

    @Nested
    @DisplayName("getById 获取渠道")
    class GetByIdTests {

        @Test
        @DisplayName("透传渠道基础字段到响应")
        void getById_passesBasicFieldsToResponse() {
            Channel channel = buildChannel(1L, "ch-1");
            when(channelRepository.findById(1L)).thenReturn(Optional.of(channel));

            ChannelView result = channelService.getById(1L);

            assertThat(result).isNotNull();
            assertThat(result.getChannel().getId()).isEqualTo(1L);
            assertThat(result.getChannel().getName()).isEqualTo("ch-1");
        }

        @Test
        @DisplayName("渠道不存在时抛 CHANNEL_NOT_FOUND")
        void getById_missing_throws() {
            when(channelRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> channelService.getById(99L))
                    .isInstanceOf(GatewayRequestException.class)
                    .satisfies(ex -> assertThat(((GatewayRequestException) ex).getCode())
                            .isEqualTo("CHANNEL_NOT_FOUND"));
        }
    }

    // ==================== create 测试 ====================

    @Nested
    @DisplayName("create 创建渠道")
    class CreateTests {

        @Test
        @DisplayName("名称重复时抛 CHANNEL_NAME_DUPLICATE")
        void duplicateName_throws() {
            when(channelRepository.existsByProviderIdAndName(10L, "ch-1")).thenReturn(true);

            assertThatThrownBy(() -> channelService.create(request("ch-1")))
                    .isInstanceOf(GatewayRequestException.class)
                    .satisfies(ex -> assertThat(((GatewayRequestException) ex).getCode())
                            .isEqualTo("CHANNEL_NAME_DUPLICATE"));
            verify(channelRepository, never()).save(any(Channel.class));
        }

        @Test
        @DisplayName("创建成功并透传计费模式（不区分大小写）")
        void create_success() {
            when(channelRepository.existsByProviderIdAndName(10L, "ch-1")).thenReturn(false);
            when(channelRepository.save(any(Channel.class))).thenAnswer(inv -> {
                Channel c = inv.getArgument(0);
                c.setId(5L);
                return c;
            });
            ChannelView result = channelService.create(request("pay_as_you_go"));

            assertThat(result.getChannel().getId()).isEqualTo(5L);
            assertThat(result.getChannel().getName()).isEqualTo("ch-1");
            assertThat(result.getChannel().getBillingMode()).isEqualTo(BillingMode.PAY_AS_YOU_GO);
            assertThat(result.getChannel().getState()).isEqualTo(ChannelState.ACTIVE);
            ArgumentCaptor<Channel> captor = ArgumentCaptor.forClass(Channel.class);
            verify(channelRepository).save(captor.capture());
            assertThat(captor.getValue().getBillingMode()).isEqualTo(BillingMode.PAY_AS_YOU_GO);
            assertThat(captor.getValue().getState()).isEqualTo(ChannelState.ACTIVE);
            assertThat(captor.getValue().getProviderId()).isEqualTo(10L);
        }
    }

    // ==================== update 测试 ====================

    @Nested
    @DisplayName("update 更新渠道")
    class UpdateTests {

        @Test
        @DisplayName("渠道不存在时抛 CHANNEL_NOT_FOUND")
        void update_missing_throws() {
            when(channelRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> channelService.update(99L, request("ch-1")))
                    .isInstanceOf(GatewayRequestException.class)
                    .satisfies(ex -> assertThat(((GatewayRequestException) ex).getCode())
                            .isEqualTo("CHANNEL_NOT_FOUND"));
        }

        @Test
        @DisplayName("名称变更且与其他渠道重复时抛 CHANNEL_NAME_DUPLICATE")
        void update_duplicateName_throws() {
            Channel channel = buildChannel(1L, "old-name");
            when(channelRepository.findById(1L)).thenReturn(Optional.of(channel));
            when(channelRepository.existsByProviderIdAndName(10L, "ch-1")).thenReturn(true);

            assertThatThrownBy(() -> channelService.update(1L, request("ch-1")))
                    .isInstanceOf(GatewayRequestException.class)
                    .satisfies(ex -> assertThat(((GatewayRequestException) ex).getCode())
                            .isEqualTo("CHANNEL_NAME_DUPLICATE"));
            verify(channelRepository, never()).save(any(Channel.class));
        }

        @Test
        @DisplayName("更新成功（名称未变更时跳过重复校验）")
        void update_success_sameName() {
            Channel channel = buildChannel(1L, "ch-1");
            when(channelRepository.findById(1L)).thenReturn(Optional.of(channel));
            when(channelRepository.save(any(Channel.class))).thenReturn(channel);
            ChannelCommand request = new ChannelCommand(10L, "ch-1", "pay_as_you_go", 1000L, null, null);

            ChannelView result = channelService.update(1L, request);

            assertThat(result.getChannel().getName()).isEqualTo("ch-1");
            assertThat(result.getChannel().getQuotaLimit()).isEqualTo(1000L);
            // 名称未变 → 不触发重复校验
            verify(channelRepository, never()).existsByProviderIdAndName(any(), any());
        }
    }

    // ==================== 查询列表测试 ====================

    @Nested
    @DisplayName("列表查询")
    class ListTests {

        @Test
        @DisplayName("getAll 返回全部渠道")
        void getAll() {
            Channel c1 = buildChannel(1L, "ch-1");
            Channel c2 = buildChannel(2L, "ch-2");
            when(channelRepository.findAll()).thenReturn(List.of(c1, c2));

            List<ChannelView> result = channelService.getAll();

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getChannel().getName()).isEqualTo("ch-1");
            assertThat(result.get(1).getChannel().getName()).isEqualTo("ch-2");
        }

        @Test
        @DisplayName("getByProviderId 按供应商过滤")
        void getByProviderId() {
            Channel c1 = buildChannel(1L, "ch-1");
            when(channelRepository.findByProviderId(10L)).thenReturn(List.of(c1));

            List<ChannelView> result = channelService.getByProviderId(10L);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getChannel().getProviderId()).isEqualTo(10L);
        }

        @Test
        @DisplayName("getByProviderIdAndBillingMode 按供应商与计费模式过滤")
        void getByProviderIdAndBillingMode() {
            Channel c1 = buildChannel(1L, "ch-1");
            when(channelRepository.findByProviderIdAndBillingMode(10L, BillingMode.PAY_AS_YOU_GO))
                    .thenReturn(List.of(c1));

            List<ChannelView> result =
                    channelService.getByProviderIdAndBillingMode(10L, BillingMode.PAY_AS_YOU_GO);

            assertThat(result).hasSize(1);
        }
    }

    // ==================== delete 测试 ====================

    @Nested
    @DisplayName("delete 删除渠道")
    class DeleteTests {

        @Test
        @DisplayName("删除成功")
        void delete_success() {
            channelService.delete(1L);

            verify(channelRepository).deleteById(1L);
        }
    }

    // ==================== addEndpoint 测试 ====================

    @Nested
    @DisplayName("addEndpoint 添加端点")
    class AddEndpointTests {

        @Test
        @DisplayName("渠道不存在时抛 IllegalArgumentException")
        void channelNotFound_throws() {
            when(channelRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> channelService.addEndpoint(endpointRequest(99L, "openai", "https://a.com")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("渠道不存在");
        }

        @Test
        @DisplayName("协议为空时抛异常")
        void blankProtocol_throws() {
            Channel channel = buildChannel(1L, "ch-1");
            when(channelRepository.findById(1L)).thenReturn(Optional.of(channel));

            assertThatThrownBy(() -> channelService.addEndpoint(endpointRequest(1L, " ", "https://a.com")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("协议不能为空");
        }

        @Test
        @DisplayName("URL 为空时抛异常")
        void blankUrl_throws() {
            Channel channel = buildChannel(1L, "ch-1");
            when(channelRepository.findById(1L)).thenReturn(Optional.of(channel));

            assertThatThrownBy(() -> channelService.addEndpoint(endpointRequest(1L, "openai", "  ")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("端点 URL 不能为空");
        }

        @Test
        @DisplayName("同渠道同协议重复时抛异常")
        void duplicateProtocol_throws() {
            Channel channel = buildChannel(1L, "ch-1");
            when(channelRepository.findById(1L)).thenReturn(Optional.of(channel));
            ChannelEndpoint existing = new ChannelEndpoint();
            existing.setId(100L);
            existing.setChannelId(1L);
            existing.setProtocol(Protocol.OPENAI);
            when(channelEndpointRepository.findByChannelId(1L)).thenReturn(List.of(existing));

            assertThatThrownBy(() -> channelService.addEndpoint(endpointRequest(1L, "openai", "https://b.com")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("已存在该协议端点");
        }

        @Test
        @DisplayName("同渠道相同 URL 重复时抛异常")
        void duplicateUrl_throws() {
            Channel channel = buildChannel(1L, "ch-1");
            when(channelRepository.findById(1L)).thenReturn(Optional.of(channel));
            ChannelEndpoint existing = new ChannelEndpoint();
            existing.setId(100L);
            existing.setChannelId(1L);
            existing.setProtocol(Protocol.ANTHROPIC);
            existing.setEndpointUrl("https://a.com");
            when(channelEndpointRepository.findByChannelId(1L)).thenReturn(List.of(existing));

            assertThatThrownBy(() -> channelService.addEndpoint(endpointRequest(1L, "openai", " https://a.com ")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("已存在相同 URL 的端点");
        }

        @Test
        @DisplayName("添加端点成功")
        void addEndpoint_success() {
            Channel channel = buildChannel(1L, "ch-1");
            when(channelRepository.findById(1L)).thenReturn(Optional.of(channel));
            when(channelEndpointRepository.findByChannelId(1L)).thenReturn(List.of());
            when(channelEndpointRepository.save(any(ChannelEndpoint.class))).thenAnswer(inv -> {
                ChannelEndpoint ep = inv.getArgument(0);
                ep.setId(300L);
                return ep;
            });

            ChannelEndpoint result =
                    channelService.addEndpoint(endpointRequest(1L, "openai", "https://a.com"));

            assertThat(result.getId()).isEqualTo(300L);
            assertThat(result.getProtocol()).isEqualTo(Protocol.OPENAI);
            assertThat(result.getEndpointUrl()).isEqualTo("https://a.com");
        }
    }

    // ==================== updateEndpoint 测试 ====================

    @Nested
    @DisplayName("updateEndpoint 更新端点")
    class UpdateEndpointTests {

        @Test
        @DisplayName("端点不存在时抛异常")
        void endpointNotFound_throws() {
            when(channelEndpointRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> channelService.updateEndpoint(1L, 99L, endpointRequest(1L, "openai", "https://a.com")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("端点不存在");
        }

        @Test
        @DisplayName("端点不属于该渠道时抛异常")
        void endpointNotBelonging_throws() {
            ChannelEndpoint existing = new ChannelEndpoint();
            existing.setId(100L);
            existing.setChannelId(2L);
            existing.setProtocol(Protocol.OPENAI);
            when(channelEndpointRepository.findById(100L)).thenReturn(Optional.of(existing));

            assertThatThrownBy(() -> channelService.updateEndpoint(1L, 100L, endpointRequest(1L, "openai", "https://a.com")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("端点不属于该渠道");
        }

        @Test
        @DisplayName("更新端点成功（排除自身后的唯一性校验通过）")
        void updateEndpoint_success() {
            ChannelEndpoint existing = new ChannelEndpoint();
            existing.setId(100L);
            existing.setChannelId(1L);
            existing.setProtocol(Protocol.OPENAI);
            existing.setEndpointUrl("https://old.com");
            when(channelEndpointRepository.findById(100L)).thenReturn(Optional.of(existing));
            // 同渠道已有自身 + 另一个 ANTHROPIC 端点；更新后 OPENAI 唯一性不受影响
            ChannelEndpoint other = new ChannelEndpoint();
            other.setId(200L);
            other.setChannelId(1L);
            other.setProtocol(Protocol.ANTHROPIC);
            other.setEndpointUrl("https://other.com");
            when(channelEndpointRepository.findByChannelId(1L)).thenReturn(List.of(existing, other));
            when(channelEndpointRepository.save(any(ChannelEndpoint.class))).thenAnswer(inv -> inv.getArgument(0));

            ChannelEndpoint result =
                    channelService.updateEndpoint(1L, 100L, endpointRequest(1L, "openai", "https://new.com"));

            assertThat(result.getEndpointUrl()).isEqualTo("https://new.com");
            assertThat(existing.getProtocol()).isEqualTo(Protocol.OPENAI);
        }
    }

    // ==================== removeEndpoint 测试 ====================

    @Nested
    @DisplayName("removeEndpoint 删除端点")
    class RemoveEndpointTests {

        @Test
        @DisplayName("端点不存在时抛异常")
        void endpointNotFound_throws() {
            when(channelEndpointRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> channelService.removeEndpoint(1L, 99L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("端点不存在");
        }

        @Test
        @DisplayName("端点不属于该渠道时抛异常")
        void endpointNotBelonging_throws() {
            ChannelEndpoint existing = new ChannelEndpoint();
            existing.setId(100L);
            existing.setChannelId(2L);
            when(channelEndpointRepository.findById(100L)).thenReturn(Optional.of(existing));

            assertThatThrownBy(() -> channelService.removeEndpoint(1L, 100L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("端点不属于该渠道");
        }

        @Test
        @DisplayName("删除端点成功")
        void removeEndpoint_success() {
            ChannelEndpoint existing = new ChannelEndpoint();
            existing.setId(100L);
            existing.setChannelId(1L);
            when(channelEndpointRepository.findById(100L)).thenReturn(Optional.of(existing));

            channelService.removeEndpoint(1L, 100L);

            verify(channelEndpointRepository).deleteById(100L);
        }
    }

    // ==================== 辅助方法 ====================

    /** 构造最小可用渠道实体（state=ACTIVE，无端点） */
    private Channel buildChannel(Long id, String name) {
        Channel channel = new Channel();
        channel.setId(id);
        channel.setProviderId(10L);
        channel.setName(name);
        channel.setBillingMode(BillingMode.PAY_AS_YOU_GO);
        channel.setState(ChannelState.ACTIVE);
        return channel;
    }

    private ChannelCommand request(String billingMode) {
        return new ChannelCommand(10L, "ch-1", billingMode, null, null, null);
    }

    private ChannelEndpointCommand endpointRequest(Long channelId, String protocol, String url) {
        return new ChannelEndpointCommand(channelId, protocol, url);
    }
}
