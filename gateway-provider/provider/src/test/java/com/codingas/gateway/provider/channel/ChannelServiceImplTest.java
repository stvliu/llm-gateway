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

import com.codingas.gateway.common.exception.DuplicateResourceException;
import com.codingas.gateway.common.exception.GatewayRequestException;
import com.codingas.gateway.common.exception.ResourceNotFoundException;
import com.codingas.gateway.provider.model.BillingMode;
import com.codingas.gateway.provider.model.ModelInstance;
import com.codingas.gateway.provider.model.ModelInstanceRepository;
import com.codingas.gateway.protocol.Protocol;
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

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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

    @Mock
    private ChannelCredentialService channelCredentialService;

    @InjectMocks
    private ChannelServiceImpl channelService;

    // ==================== getById 测试 ====================

    @Nested
    @DisplayName("getById 获取渠道")
    class GetByIdTests {

        @Test
        @DisplayName("透传渠道基础字段到响应")
        void getById_passesBasicFieldsToResponse() {
            Channel channel = buildChannel(1L, "ch-1");
            when(channelRepository.findById(1L)).thenReturn(Optional.of(channel));

            Channel result = channelService.getById(1L);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getName()).isEqualTo("ch-1");
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

            assertThatThrownBy(() -> channelService.create(channelEntity()))
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
            Channel result = channelService.create(channelEntity("pay_as_you_go"));

            assertThat(result.getId()).isEqualTo(5L);
            assertThat(result.getName()).isEqualTo("ch-1");
            assertThat(result.getBillingMode()).isEqualTo(BillingMode.PAY_AS_YOU_GO);
            assertThat(result.getState()).isEqualTo(ChannelState.ACTIVE);
            ArgumentCaptor<Channel> captor = ArgumentCaptor.forClass(Channel.class);
            verify(channelRepository).save(captor.capture());
            assertThat(captor.getValue().getBillingMode()).isEqualTo(BillingMode.PAY_AS_YOU_GO);
            assertThat(captor.getValue().getState()).isEqualTo(ChannelState.ACTIVE);
            assertThat(captor.getValue().getProviderId()).isEqualTo(10L);
        }
    }

    // ==================== copy 测试 ====================

    @Nested
    @DisplayName("copy 复制渠道")
    class CopyTests {

        @Test
        @DisplayName("复制继承源配置并重置状态与健康字段")
        void copy_inheritsConfigAndResetsState() {
            // given：源渠道（含计费/配额/超时/重试/健康字段）+ 端点 + 模型实例
            Channel source = buildChannel(1L, "ch-1");
            source.setQuotaLimit(1000L);
            source.setTimeout(60);
            source.setMaxRetries(3);
            source.setLastHealthCheckAt(Instant.parse("2026-01-01T00:00:00Z"));
            source.setLastHealthStatus(ChannelHealthStatus.HEALTHY);
            source.setLastHealthSource(ChannelHealthSource.CARD);
            when(channelRepository.findById(1L)).thenReturn(Optional.of(source));

            ChannelEndpoint ep = new ChannelEndpoint();
            ep.setId(100L);
            ep.setChannelId(1L);
            ep.setProtocol(Protocol.OPENAI);
            ep.setEndpointUrl("https://a.com");
            when(channelEndpointRepository.findByChannelId(1L)).thenReturn(List.of(ep));

            ModelInstance mi = new ModelInstance();
            mi.setId(200L);
            mi.setChannelId(1L);
            mi.setModelId(5L);
            mi.setUpstreamModelName("gpt-4");
            mi.setState(ModelInstance.State.ACTIVE);
            when(modelInstanceRepository.findByChannelId(1L)).thenReturn(List.of(mi));

            when(channelRepository.save(any(Channel.class))).thenAnswer(inv -> {
                Channel c = inv.getArgument(0);
                c.setId(9L);
                return c;
            });

            Channel override = new Channel();
            override.setName("ch-copy");

            // when
            Channel result = channelService.copy(1L, override, false);

            // then：继承本体 + 覆盖 name + 重置
            assertThat(result.getId()).isEqualTo(9L);
            assertThat(result.getName()).isEqualTo("ch-copy");
            assertThat(result.getProviderId()).isEqualTo(10L);
            assertThat(result.getBillingMode()).isEqualTo(BillingMode.PAY_AS_YOU_GO);
            assertThat(result.getQuotaLimit()).isEqualTo(1000L);
            assertThat(result.getTimeout()).isEqualTo(60);
            assertThat(result.getMaxRetries()).isEqualTo(3);
            assertThat(result.getState()).isEqualTo(ChannelState.ACTIVE);
            assertThat(result.getLastHealthCheckAt()).isNull();
            assertThat(result.getLastHealthStatus()).isNull();
            assertThat(result.getLastHealthSource()).isNull();

            // 端点复制（指向新渠道）
            ArgumentCaptor<ChannelEndpoint> epCaptor = ArgumentCaptor.forClass(ChannelEndpoint.class);
            verify(channelEndpointRepository).save(epCaptor.capture());
            assertThat(epCaptor.getValue().getChannelId()).isEqualTo(9L);
            assertThat(epCaptor.getValue().getProtocol()).isEqualTo(Protocol.OPENAI);
            assertThat(epCaptor.getValue().getEndpointUrl()).isEqualTo("https://a.com");

            // 模型实例复制（重置为 ACTIVE 可用状态）
            ArgumentCaptor<ModelInstance> miCaptor = ArgumentCaptor.forClass(ModelInstance.class);
            verify(modelInstanceRepository).save(miCaptor.capture());
            assertThat(miCaptor.getValue().getChannelId()).isEqualTo(9L);
            assertThat(miCaptor.getValue().getModelId()).isEqualTo(5L);
            assertThat(miCaptor.getValue().getState()).isEqualTo(ModelInstance.State.ACTIVE);

            // 默认不复制凭证
            verify(channelCredentialService, never()).create(any(ChannelCredential.class));
        }

        @Test
        @DisplayName("copyCredentials=true 时复制凭证（复用明文重新加密）")
        void copy_withCredentials_copiesCredentials() {
            Channel source = buildChannel(1L, "ch-1");
            when(channelRepository.findById(1L)).thenReturn(Optional.of(source));
            when(channelEndpointRepository.findByChannelId(1L)).thenReturn(List.of());
            when(modelInstanceRepository.findByChannelId(1L)).thenReturn(List.of());
            when(channelRepository.save(any(Channel.class))).thenAnswer(inv -> {
                Channel c = inv.getArgument(0);
                c.setId(9L);
                return c;
            });

            ChannelCredential cred = new ChannelCredential();
            cred.setId(300L);
            cred.setChannelId(1L);
            cred.setName("key-1");
            cred.setApiKeyPlain("sk-1234567890");
            cred.setWeight(1);
            cred.setPriority(1);
            when(channelCredentialService.listByChannelId(1L)).thenReturn(List.of(cred));
            when(channelCredentialService.create(any(ChannelCredential.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            Channel override = new Channel();
            override.setName("ch-copy");

            channelService.copy(1L, override, true);

            ArgumentCaptor<ChannelCredential> captor = ArgumentCaptor.forClass(ChannelCredential.class);
            verify(channelCredentialService).create(captor.capture());
            assertThat(captor.getValue().getChannelId()).isEqualTo(9L);
            assertThat(captor.getValue().getName()).isEqualTo("key-1");
            assertThat(captor.getValue().getApiKeyPlain()).isEqualTo("sk-1234567890");
            assertThat(captor.getValue().getWeight()).isEqualTo(1);
            assertThat(captor.getValue().getPriority()).isEqualTo(1);
        }

        @Test
        @DisplayName("源渠道不存在抛 ResourceNotFoundException")
        void copy_sourceNotFound_throws() {
            when(channelRepository.findById(99L)).thenReturn(Optional.empty());

            Channel override = new Channel();
            override.setName("x");

            assertThatThrownBy(() -> channelService.copy(99L, override, false))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("同供应商渠道重名抛 DuplicateResourceException")
        void copy_duplicateName_throws() {
            Channel source = buildChannel(1L, "ch-1");
            when(channelRepository.findById(1L)).thenReturn(Optional.of(source));
            when(channelRepository.existsByProviderIdAndName(10L, "ch-1")).thenReturn(true);

            Channel override = new Channel();
            override.setName("ch-1");

            assertThatThrownBy(() -> channelService.copy(1L, override, false))
                    .isInstanceOf(DuplicateResourceException.class);
            verify(channelRepository, never()).save(any(Channel.class));
        }

        @Test
        @DisplayName("凭证复制失败时异常向上传播（事务回滚由 @Transactional 保证）")
        void copy_credentialFailure_propagates() {
            Channel source = buildChannel(1L, "ch-1");
            when(channelRepository.findById(1L)).thenReturn(Optional.of(source));
            when(channelEndpointRepository.findByChannelId(1L)).thenReturn(List.of());
            when(modelInstanceRepository.findByChannelId(1L)).thenReturn(List.of());
            when(channelRepository.save(any(Channel.class))).thenAnswer(inv -> inv.getArgument(0));

            ChannelCredential cred = new ChannelCredential();
            cred.setChannelId(1L);
            cred.setApiKeyPlain("sk-1234567890");
            when(channelCredentialService.listByChannelId(1L)).thenReturn(List.of(cred));
            when(channelCredentialService.create(any(ChannelCredential.class)))
                    .thenThrow(new IllegalArgumentException("加密失败"));

            Channel override = new Channel();
            override.setName("ch-copy");

            assertThatThrownBy(() -> channelService.copy(1L, override, true))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("加密失败");
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

            assertThatThrownBy(() -> channelService.update(99L, channelEntity()))
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

            assertThatThrownBy(() -> channelService.update(1L, channelEntity()))
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
            Channel request = channelEntity("pay_as_you_go");
            request.setQuotaLimit(1000L);

            Channel result = channelService.update(1L, request);

            assertThat(result.getName()).isEqualTo("ch-1");
            assertThat(result.getQuotaLimit()).isEqualTo(1000L);
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

            List<Channel> result = channelService.getAll();

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getName()).isEqualTo("ch-1");
            assertThat(result.get(1).getName()).isEqualTo("ch-2");
        }

        @Test
        @DisplayName("getByProviderId 按供应商过滤")
        void getByProviderId() {
            Channel c1 = buildChannel(1L, "ch-1");
            when(channelRepository.findByProviderId(10L)).thenReturn(List.of(c1));

            List<Channel> result = channelService.getByProviderId(10L);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getProviderId()).isEqualTo(10L);
        }

        @Test
        @DisplayName("getByProviderIdAndBillingMode 按供应商与计费模式过滤")
        void getByProviderIdAndBillingMode() {
            Channel c1 = buildChannel(1L, "ch-1");
            when(channelRepository.findByProviderIdAndBillingMode(10L, BillingMode.PAY_AS_YOU_GO))
                    .thenReturn(List.of(c1));

            List<Channel> result =
                    channelService.getByProviderIdAndBillingMode(10L, BillingMode.PAY_AS_YOU_GO);

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("getAll 默认按渠道名升序排序")
        void getAll_defaultSortByNameAsc() {
            Channel c1 = buildChannel(1L, "zeta");
            Channel c2 = buildChannel(2L, "alpha");
            when(channelRepository.findAll()).thenReturn(List.of(c1, c2));

            List<Channel> result = channelService.getAll();

            assertThat(result).extracting(Channel::getName).containsExactly("alpha", "zeta");
        }

        @Test
        @DisplayName("getAll 支持按名称降序排序")
        void getAll_sortByNameDesc() {
            Channel c1 = buildChannel(1L, "beta");
            Channel c2 = buildChannel(2L, "alpha");
            when(channelRepository.findAll()).thenReturn(List.of(c1, c2));

            List<Channel> result = channelService.getAll("name", "DESC");

            assertThat(result).extracting(Channel::getName).containsExactly("beta", "alpha");
        }

        @Test
        @DisplayName("getAll 非法排序字段回退默认名称升序（防注入）")
        void getAll_invalidSortBy_fallsBackToNameAsc() {
            Channel c1 = buildChannel(1L, "beta");
            Channel c2 = buildChannel(2L, "alpha");
            when(channelRepository.findAll()).thenReturn(List.of(c1, c2));

            List<Channel> result = channelService.getAll("name; DROP TABLE channels", "ASC");

            assertThat(result).extracting(Channel::getName).containsExactly("alpha", "beta");
        }

        @Test
        @DisplayName("getByProviderId 支持按名称升序排序")
        void getByProviderId_sortByNameAsc() {
            Channel c1 = buildChannel(1L, "zeta");
            Channel c2 = buildChannel(2L, "alpha");
            when(channelRepository.findByProviderId(10L)).thenReturn(List.of(c1, c2));

            List<Channel> result = channelService.getByProviderId(10L, "name", "ASC");

            assertThat(result).extracting(Channel::getName).containsExactly("alpha", "zeta");
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

            assertThatThrownBy(() -> channelService.addEndpoint(endpointEntity(99L, "openai", "https://a.com")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("渠道不存在");
        }

        @Test
        @DisplayName("协议为空时抛异常")
        void blankProtocol_throws() {
            Channel channel = buildChannel(1L, "ch-1");
            when(channelRepository.findById(1L)).thenReturn(Optional.of(channel));

            assertThatThrownBy(() -> channelService.addEndpoint(endpointEntity(1L, null, "https://a.com")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("协议不能为空");
        }

        @Test
        @DisplayName("URL 为空时抛异常")
        void blankUrl_throws() {
            Channel channel = buildChannel(1L, "ch-1");
            when(channelRepository.findById(1L)).thenReturn(Optional.of(channel));

            assertThatThrownBy(() -> channelService.addEndpoint(endpointEntity(1L, "openai", "  ")))
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

            assertThatThrownBy(() -> channelService.addEndpoint(endpointEntity(1L, "openai", "https://b.com")))
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

            assertThatThrownBy(() -> channelService.addEndpoint(endpointEntity(1L, "openai", " https://a.com ")))
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
                    channelService.addEndpoint(endpointEntity(1L, "openai", "https://a.com"));

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

            assertThatThrownBy(() -> channelService.updateEndpoint(1L, 99L, endpointEntity(1L, "openai", "https://a.com")))
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

            assertThatThrownBy(() -> channelService.updateEndpoint(1L, 100L, endpointEntity(1L, "openai", "https://a.com")))
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
                    channelService.updateEndpoint(1L, 100L, endpointEntity(1L, "openai", "https://new.com"));

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

    private Channel channelEntity() {
        Channel channel = new Channel();
        channel.setProviderId(10L);
        channel.setName("ch-1");
        channel.setBillingMode(BillingMode.PAY_AS_YOU_GO);
        return channel;
    }

    private Channel channelEntity(String billingMode) {
        Channel channel = channelEntity();
        channel.setBillingMode(BillingMode.fromCode(billingMode));
        return channel;
    }

    private ChannelEndpoint endpointEntity(Long channelId, String protocol, String url) {
        ChannelEndpoint endpoint = new ChannelEndpoint();
        endpoint.setChannelId(channelId);
        if (protocol != null) {
            endpoint.setProtocol(Protocol.fromCode(protocol));
        }
        endpoint.setEndpointUrl(url);
        return endpoint;
    }
}
