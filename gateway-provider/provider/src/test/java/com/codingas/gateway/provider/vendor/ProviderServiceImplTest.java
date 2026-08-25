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
package com.codingas.gateway.provider.vendor;

import com.codingas.gateway.common.dto.PageResponse;
import com.codingas.gateway.common.exception.ResourceNotFoundException;
import com.codingas.gateway.provider.channel.Channel;
import com.codingas.gateway.provider.channel.ChannelCredential;
import com.codingas.gateway.provider.channel.ChannelCredentialRepository;
import com.codingas.gateway.provider.channel.ChannelRepository;
import com.codingas.gateway.provider.model.Model;
import com.codingas.gateway.provider.model.ModelRepository;
import com.codingas.gateway.provider.model.ModelInstance;
import com.codingas.gateway.provider.model.ModelInstanceRepository;
import com.codingas.gateway.provider.upstream.ConnectivityTester;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * ProviderServiceImpl 单元测试
 *
 * <p>覆盖 CRUD、分页查询、级联删除、连通性测试等全部 public 方法分支。</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ProviderServiceImpl 单元测试")
class ProviderServiceImplTest {

    @Mock
    private ProviderRepository providerRepository;
    @Mock
    private ModelRepository modelRepository;
    @Mock
    private ChannelRepository channelRepository;
    @Mock
    private ModelInstanceRepository modelInstanceRepository;
    @Mock
    private ChannelCredentialRepository channelCredentialRepository;
    @Mock
    private ConnectivityTester connectivityTester;

    private ProviderServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ProviderServiceImpl(
                providerRepository, modelRepository, channelRepository,
                modelInstanceRepository, channelCredentialRepository, connectivityTester);
    }

    // ==================== create 测试 ====================

    @Nested
    @DisplayName("create 创建供应商")
    class CreateTests {

        @Test
        @DisplayName("含嵌套模型时创建模型，priority 缺省为 100")
        void create_withNestedModels() {
            ModelNestedCommand nested = new ModelNestedCommand(
                    "gpt-4", "GPT-4", 8000, null, null, Map.of("vision", true));
            ProviderCreateCommand request = new ProviderCreateCommand(
                    "openai", "OpenAI", "https://openai.com",
                    "https://platform.openai.com/docs", null, List.of(nested));

            when(providerRepository.save(any(Provider.class))).thenAnswer(inv -> {
                Provider p = inv.getArgument(0);
                p.setId(1L);
                return p;
            });

            Provider response = service.create(request);

            assertThat(response.getId()).isEqualTo(1L);
            assertThat(response.getName()).isEqualTo("OpenAI");
            assertThat(response.getCode()).isEqualTo("openai");
            ArgumentCaptor<Provider> captor = ArgumentCaptor.forClass(Provider.class);
            verify(providerRepository).save(captor.capture());
            assertThat(captor.getValue().getCode()).isEqualTo("openai");
            assertThat(captor.getValue().getPriority()).isEqualTo(100);
            ArgumentCaptor<Model> modelCaptor = ArgumentCaptor.forClass(Model.class);
            verify(modelRepository).save(modelCaptor.capture());
            assertThat(modelCaptor.getValue().getModelName()).isEqualTo("gpt-4");
            assertThat(modelCaptor.getValue().getCapabilities()).containsEntry("vision", true);
        }

        @Test
        @DisplayName("无嵌套模型时不创建模型")
        void create_withoutModels() {
            ProviderCreateCommand request =
                    new ProviderCreateCommand("anthropic", "Anthropic", null, null, 5, null);
            when(providerRepository.save(any(Provider.class))).thenAnswer(inv -> {
                Provider p = inv.getArgument(0);
                p.setId(2L);
                return p;
            });

            Provider response = service.create(request);

            assertThat(response.getName()).isEqualTo("Anthropic");
            assertThat(response.getCode()).isEqualTo("anthropic");
            assertThat(response.getPriority()).isEqualTo(5);
            ArgumentCaptor<Provider> captor = ArgumentCaptor.forClass(Provider.class);
            verify(providerRepository).save(captor.capture());
            assertThat(captor.getValue().getCode()).isEqualTo("anthropic");
            verify(modelRepository, never()).save(any(Model.class));
        }
    }

    // ==================== getById 测试 ====================

    @Nested
    @DisplayName("getById 获取供应商")
    class GetByIdTests {

        @Test
        @DisplayName("存在时返回响应")
        void existing_returnsResponse() {
            Provider p = provider(1L, "openai", "OpenAI");
            when(providerRepository.findById(1L)).thenReturn(Optional.of(p));

            Provider response = service.getById(1L);

            assertThat(response.getId()).isEqualTo(1L);
            assertThat(response.getCode()).isEqualTo("openai");
            assertThat(response.getName()).isEqualTo("OpenAI");
        }

        @Test
        @DisplayName("不存在时抛 ResourceNotFoundException")
        void missing_throws() {
            when(providerRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getById(99L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Provider");
        }
    }

    // ==================== query 测试 ====================

    @Nested
    @DisplayName("query 分页查询")
    class QueryTests {

        @Test
        @DisplayName("无关键字时返回全部并分页")
        void noKeyword_paginated() {
            when(providerRepository.findAll()).thenReturn(List.of(
                    provider(1L, "openai", "OpenAI"),
                    provider(2L, "anthropic", "Anthropic"),
                    provider(3L, "zhipu", "智谱")));

            ProviderQuery request = new ProviderQuery();
            request.setPage(2);
            request.setLimit(2);

            PageResponse<Provider> response = service.query(request);

            assertThat(response.getItems()).hasSize(1);
            assertThat(response.getItems().get(0).getName()).isEqualTo("智谱");
            assertThat(response.getPagination().getTotal()).isEqualTo(3);
            assertThat(response.getPagination().getPage()).isEqualTo(2);
        }

        @Test
        @DisplayName("关键字过滤匹配名称（忽略大小写）")
        void keyword_filtersByName() {
            when(providerRepository.findAll()).thenReturn(List.of(
                    provider(1L, "openai", "OpenAI"),
                    provider(2L, "anthropic", "Anthropic")));

            ProviderQuery request = new ProviderQuery();
            request.setKeyword("open");

            PageResponse<Provider> response = service.query(request);

            assertThat(response.getItems()).hasSize(1);
            assertThat(response.getItems().get(0).getName()).isEqualTo("OpenAI");
            assertThat(response.getPagination().getTotal()).isEqualTo(1);
        }

        @Test
        @DisplayName("关键字不匹配时返回空列表")
        void keyword_noMatch() {
            when(providerRepository.findAll()).thenReturn(List.of(provider(1L, "openai", "OpenAI")));

            ProviderQuery request = new ProviderQuery();
            request.setKeyword("claude");

            PageResponse<Provider> response = service.query(request);

            assertThat(response.getItems()).isEmpty();
            assertThat(response.getPagination().getTotal()).isZero();
        }
    }

    // ==================== update 测试 ====================

    @Nested
    @DisplayName("update 更新供应商")
    class UpdateTests {

        @Test
        @DisplayName("全字段更新")
        void allFieldsUpdated() {
            Provider p = provider(1L, "openai", "OpenAI");
            when(providerRepository.findById(1L)).thenReturn(Optional.of(p));
            when(providerRepository.save(any(Provider.class))).thenReturn(p);

            ProviderUpdateCommand request = new ProviderUpdateCommand(
                    "OpenAI 2", "https://openai.com/v2", "https://platform.openai.com/docs/v2", 10);

            Provider response = service.update(1L, request);

            assertThat(response.getName()).isEqualTo("OpenAI 2");
            assertThat(p.getWebsiteUrl()).isEqualTo("https://openai.com/v2");
            assertThat(p.getApiDocUrl()).isEqualTo("https://platform.openai.com/docs/v2");
            assertThat(p.getPriority()).isEqualTo(10);
        }

        @Test
        @DisplayName("仅更新非 null 字段")
        void partialFieldsUpdated() {
            Provider p = provider(1L, "openai", "OpenAI");
            p.setPriority(3);
            when(providerRepository.findById(1L)).thenReturn(Optional.of(p));
            when(providerRepository.save(any(Provider.class))).thenReturn(p);

            ProviderUpdateCommand request = new ProviderUpdateCommand("New Name", null, null, null);

            Provider response = service.update(1L, request);

            assertThat(response.getName()).isEqualTo("New Name");
            // 其余字段保持不变
            assertThat(p.getWebsiteUrl()).isNull();
            assertThat(p.getPriority()).isEqualTo(3);
        }

        @Test
        @DisplayName("不存在时抛 ResourceNotFoundException")
        void missing_throws() {
            when(providerRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.update(99L, new ProviderUpdateCommand(null, null, null, null)))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ==================== delete 测试 ====================

    @Nested
    @DisplayName("delete 级联删除")
    class DeleteTests {

        @Test
        @DisplayName("级联删除模型实例、凭证、渠道与供应商")
        void cascadeDelete() {
            Provider p = provider(1L, "openai", "OpenAI");
            when(providerRepository.findById(1L)).thenReturn(Optional.of(p));

            Channel c1 = new Channel();
            c1.setId(10L);
            Channel c2 = new Channel();
            c2.setId(20L);
            when(channelRepository.findByProviderId(1L)).thenReturn(List.of(c1, c2));

            ModelInstance mi = new ModelInstance();
            mi.setId(100L);
            when(modelInstanceRepository.findByChannelId(10L)).thenReturn(List.of(mi));
            when(modelInstanceRepository.findByChannelId(20L)).thenReturn(List.of());

            ChannelCredential cred = new ChannelCredential();
            cred.setId(200L);
            when(channelCredentialRepository.findByChannelId(10L)).thenReturn(List.of(cred));
            when(channelCredentialRepository.findByChannelId(20L)).thenReturn(List.of());

            service.delete(1L);

            verify(modelInstanceRepository).deleteById(100L);
            verify(channelCredentialRepository).deleteById(200L);
            verify(channelRepository).deleteById(10L);
            verify(channelRepository).deleteById(20L);
            verify(providerRepository).delete(p);
        }

        @Test
        @DisplayName("无渠道时仅删除供应商")
        void noChannels() {
            Provider p = provider(1L, "openai", "OpenAI");
            when(providerRepository.findById(1L)).thenReturn(Optional.of(p));
            when(channelRepository.findByProviderId(1L)).thenReturn(List.of());

            service.delete(1L);

            verify(providerRepository).delete(p);
            verify(modelInstanceRepository, never()).deleteById(any());
            verify(channelCredentialRepository, never()).deleteById(any());
        }

        @Test
        @DisplayName("不存在时抛 ResourceNotFoundException")
        void missing_throws() {
            when(providerRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.delete(99L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ==================== getProviderNames 测试 ====================

    @Nested
    @DisplayName("getProviderNames 名称列表")
    class GetProviderNamesTests {

        @Test
        @DisplayName("返回去重后的名称列表")
        void returnsDistinctNames() {
            when(providerRepository.findAll()).thenReturn(List.of(
                    provider(1L, "openai", "OpenAI"),
                    provider(2L, "openai2", "OpenAI"),
                    provider(3L, "anthropic", "Anthropic")));

            List<String> names = service.getProviderNames();

            assertThat(names).containsExactly("OpenAI", "Anthropic");
        }
    }

    // ==================== testConnectivity 测试 ====================

    @Nested
    @DisplayName("testConnectivity 连通性测试")
    class TestConnectivityTests {

        @Test
        @DisplayName("成功时返回默认成功消息")
        void success_returnsDefaultMessage() {
            when(connectivityTester.test("https://api.openai.com", "sk-123", "openai"))
                    .thenReturn(new com.codingas.gateway.protocol.transport.ConnectivityTestResult(
                            true, null, null, 120L));

            ConnectivityTestResult result = service.testConnectivity(
                    new ConnectivityTestCommand("openai", "https://api.openai.com", "sk-123", null));

            assertThat(result.success()).isTrue();
            assertThat(result.message()).isEqualTo("连通性测试成功");
            assertThat(result.level1().message()).isEqualTo("认证成功");
            assertThat(result.level1().success()).isTrue();
            assertThat(result.level1().latencyMs()).isEqualTo(120L);
            assertThat(result.totalLatencyMs()).isEqualTo(120L);
        }

        @Test
        @DisplayName("失败时透传错误消息")
        void failure_passesErrorMessage() {
            when(connectivityTester.test("https://api.openai.com", "sk-bad", "openai"))
                    .thenReturn(new com.codingas.gateway.protocol.transport.ConnectivityTestResult(
                            false, null, "401 Unauthorized", 0L));

            ConnectivityTestResult result = service.testConnectivity(
                    new ConnectivityTestCommand("openai", "https://api.openai.com", "sk-bad", null));

            assertThat(result.success()).isFalse();
            assertThat(result.message()).isEqualTo("401 Unauthorized");
            assertThat(result.level1().message()).isEqualTo("401 Unauthorized");
        }
    }

    // ==================== 辅助方法 ====================

    private Provider provider(Long id, String code, String name) {
        Provider p = new Provider();
        p.setId(id);
        p.setCode(code);
        p.setName(name);
        return p;
    }
}
