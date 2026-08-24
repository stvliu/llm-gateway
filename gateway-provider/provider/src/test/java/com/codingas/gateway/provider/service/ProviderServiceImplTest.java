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
package com.codingas.gateway.provider.service;

import com.codingas.gateway.common.dto.PageResponse;
import com.codingas.gateway.common.exception.ResourceNotFoundException;
import com.codingas.gateway.provider.channel.Channel;
import com.codingas.gateway.provider.channel.ChannelCredential;
import com.codingas.gateway.provider.channel.ChannelCredentialGateway;
import com.codingas.gateway.provider.channel.ChannelGateway;
import com.codingas.gateway.provider.model.Model;
import com.codingas.gateway.provider.model.ModelGateway;
import com.codingas.gateway.provider.model.ModelInstance;
import com.codingas.gateway.provider.model.ModelInstanceGateway;
import com.codingas.gateway.provider.upstream.ConnectivityTester;
import com.codingas.gateway.provider.vendor.ConnectivityTestRequest;
import com.codingas.gateway.provider.vendor.ConnectivityTestResult;
import com.codingas.gateway.provider.vendor.ModelNestedRequest;
import com.codingas.gateway.provider.vendor.Provider;
import com.codingas.gateway.provider.vendor.ProviderCreateRequest;
import com.codingas.gateway.provider.vendor.ProviderGateway;
import com.codingas.gateway.provider.vendor.ProviderQueryRequest;
import com.codingas.gateway.provider.vendor.ProviderResponse;
import com.codingas.gateway.provider.vendor.ProviderUpdateRequest;
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
    private ProviderGateway providerGateway;
    @Mock
    private ModelGateway modelGateway;
    @Mock
    private ChannelGateway channelGateway;
    @Mock
    private ModelInstanceGateway modelInstanceGateway;
    @Mock
    private ChannelCredentialGateway channelCredentialGateway;
    @Mock
    private ConnectivityTester connectivityTester;

    private ProviderServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ProviderServiceImpl(
                providerGateway, modelGateway, channelGateway,
                modelInstanceGateway, channelCredentialGateway, connectivityTester);
    }

    // ==================== create 测试 ====================

    @Nested
    @DisplayName("create 创建供应商")
    class CreateTests {

        @Test
        @DisplayName("含嵌套模型时创建模型，priority 缺省为 100")
        void create_withNestedModels() {
            ProviderCreateRequest request = new ProviderCreateRequest();
            request.setProviderName("OpenAI");
            request.setCode("openai");
            request.setWebsiteUrl("https://openai.com");
            request.setApiDocUrl("https://platform.openai.com/docs");
            request.setPriority(null);
            ModelNestedRequest nested = new ModelNestedRequest();
            nested.setModelName("gpt-4");
            nested.setDisplayName("GPT-4");
            nested.setContextWindow(8000);
            nested.setCapabilities(Map.of("vision", true));
            request.setModels(List.of(nested));

            when(providerGateway.save(any(Provider.class))).thenAnswer(inv -> {
                Provider p = inv.getArgument(0);
                p.setId(1L);
                return p;
            });

            ProviderResponse response = service.create(request);

            assertThat(response.getId()).isEqualTo(1L);
            assertThat(response.getProviderName()).isEqualTo("OpenAI");
            assertThat(response.getProviderId()).isEqualTo("openai");
            ArgumentCaptor<Provider> captor = ArgumentCaptor.forClass(Provider.class);
            verify(providerGateway).save(captor.capture());
            assertThat(captor.getValue().getCode()).isEqualTo("openai");
            assertThat(captor.getValue().getPriority()).isEqualTo(100);
            ArgumentCaptor<Model> modelCaptor = ArgumentCaptor.forClass(Model.class);
            verify(modelGateway).save(modelCaptor.capture());
            assertThat(modelCaptor.getValue().getModelName()).isEqualTo("gpt-4");
            assertThat(modelCaptor.getValue().getCapabilities()).containsEntry("vision", true);
        }

        @Test
        @DisplayName("无嵌套模型时不创建模型")
        void create_withoutModels() {
            ProviderCreateRequest request = new ProviderCreateRequest();
            request.setProviderName("Anthropic");
            request.setCode("anthropic");
            request.setPriority(5);
            when(providerGateway.save(any(Provider.class))).thenAnswer(inv -> {
                Provider p = inv.getArgument(0);
                p.setId(2L);
                return p;
            });

            ProviderResponse response = service.create(request);

            assertThat(response.getProviderName()).isEqualTo("Anthropic");
            assertThat(response.getProviderId()).isEqualTo("anthropic");
            assertThat(response.getPriority()).isEqualTo(5);
            ArgumentCaptor<Provider> captor = ArgumentCaptor.forClass(Provider.class);
            verify(providerGateway).save(captor.capture());
            assertThat(captor.getValue().getCode()).isEqualTo("anthropic");
            verify(modelGateway, never()).save(any(Model.class));
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
            when(providerGateway.findById(1L)).thenReturn(Optional.of(p));

            ProviderResponse response = service.getById(1L);

            assertThat(response.getId()).isEqualTo(1L);
            assertThat(response.getProviderId()).isEqualTo("openai");
            assertThat(response.getProviderName()).isEqualTo("OpenAI");
        }

        @Test
        @DisplayName("不存在时抛 ResourceNotFoundException")
        void missing_throws() {
            when(providerGateway.findById(99L)).thenReturn(Optional.empty());

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
            when(providerGateway.findAll()).thenReturn(List.of(
                    provider(1L, "openai", "OpenAI"),
                    provider(2L, "anthropic", "Anthropic"),
                    provider(3L, "zhipu", "智谱")));

            ProviderQueryRequest request = new ProviderQueryRequest();
            request.setPage(2);
            request.setLimit(2);

            PageResponse<ProviderResponse> response = service.query(request);

            assertThat(response.getItems()).hasSize(1);
            assertThat(response.getItems().get(0).getProviderName()).isEqualTo("智谱");
            assertThat(response.getPagination().getTotal()).isEqualTo(3);
            assertThat(response.getPagination().getPage()).isEqualTo(2);
        }

        @Test
        @DisplayName("关键字过滤匹配名称（忽略大小写）")
        void keyword_filtersByName() {
            when(providerGateway.findAll()).thenReturn(List.of(
                    provider(1L, "openai", "OpenAI"),
                    provider(2L, "anthropic", "Anthropic")));

            ProviderQueryRequest request = new ProviderQueryRequest();
            request.setKeyword("open");

            PageResponse<ProviderResponse> response = service.query(request);

            assertThat(response.getItems()).hasSize(1);
            assertThat(response.getItems().get(0).getProviderName()).isEqualTo("OpenAI");
            assertThat(response.getPagination().getTotal()).isEqualTo(1);
        }

        @Test
        @DisplayName("关键字不匹配时返回空列表")
        void keyword_noMatch() {
            when(providerGateway.findAll()).thenReturn(List.of(provider(1L, "openai", "OpenAI")));

            ProviderQueryRequest request = new ProviderQueryRequest();
            request.setKeyword("claude");

            PageResponse<ProviderResponse> response = service.query(request);

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
            when(providerGateway.findById(1L)).thenReturn(Optional.of(p));
            when(providerGateway.save(any(Provider.class))).thenReturn(p);

            ProviderUpdateRequest request = new ProviderUpdateRequest();
            request.setProviderName("OpenAI 2");
            request.setWebsiteUrl("https://openai.com/v2");
            request.setApiDocUrl("https://platform.openai.com/docs/v2");
            request.setPriority(10);

            ProviderResponse response = service.update(1L, request);

            assertThat(response.getProviderName()).isEqualTo("OpenAI 2");
            assertThat(p.getWebsiteUrl()).isEqualTo("https://openai.com/v2");
            assertThat(p.getApiDocUrl()).isEqualTo("https://platform.openai.com/docs/v2");
            assertThat(p.getPriority()).isEqualTo(10);
        }

        @Test
        @DisplayName("仅更新非 null 字段")
        void partialFieldsUpdated() {
            Provider p = provider(1L, "openai", "OpenAI");
            p.setPriority(3);
            when(providerGateway.findById(1L)).thenReturn(Optional.of(p));
            when(providerGateway.save(any(Provider.class))).thenReturn(p);

            ProviderUpdateRequest request = new ProviderUpdateRequest();
            request.setProviderName("New Name");

            ProviderResponse response = service.update(1L, request);

            assertThat(response.getProviderName()).isEqualTo("New Name");
            // 其余字段保持不变
            assertThat(p.getWebsiteUrl()).isNull();
            assertThat(p.getPriority()).isEqualTo(3);
        }

        @Test
        @DisplayName("不存在时抛 ResourceNotFoundException")
        void missing_throws() {
            when(providerGateway.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.update(99L, new ProviderUpdateRequest()))
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
            when(providerGateway.findById(1L)).thenReturn(Optional.of(p));

            Channel c1 = new Channel();
            c1.setId(10L);
            Channel c2 = new Channel();
            c2.setId(20L);
            when(channelGateway.findByProviderId(1L)).thenReturn(List.of(c1, c2));

            ModelInstance mi = new ModelInstance();
            mi.setId(100L);
            when(modelInstanceGateway.findByChannelId(10L)).thenReturn(List.of(mi));
            when(modelInstanceGateway.findByChannelId(20L)).thenReturn(List.of());

            ChannelCredential cred = new ChannelCredential();
            cred.setId(200L);
            when(channelCredentialGateway.findByChannelId(10L)).thenReturn(List.of(cred));
            when(channelCredentialGateway.findByChannelId(20L)).thenReturn(List.of());

            service.delete(1L);

            verify(modelInstanceGateway).deleteById(100L);
            verify(channelCredentialGateway).deleteById(200L);
            verify(channelGateway).deleteById(10L);
            verify(channelGateway).deleteById(20L);
            verify(providerGateway).delete(p);
        }

        @Test
        @DisplayName("无渠道时仅删除供应商")
        void noChannels() {
            Provider p = provider(1L, "openai", "OpenAI");
            when(providerGateway.findById(1L)).thenReturn(Optional.of(p));
            when(channelGateway.findByProviderId(1L)).thenReturn(List.of());

            service.delete(1L);

            verify(providerGateway).delete(p);
            verify(modelInstanceGateway, never()).deleteById(any());
            verify(channelCredentialGateway, never()).deleteById(any());
        }

        @Test
        @DisplayName("不存在时抛 ResourceNotFoundException")
        void missing_throws() {
            when(providerGateway.findById(99L)).thenReturn(Optional.empty());

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
            when(providerGateway.findAll()).thenReturn(List.of(
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
                    new ConnectivityTestRequest("openai", "https://api.openai.com", "sk-123", null));

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
                    new ConnectivityTestRequest("openai", "https://api.openai.com", "sk-bad", null));

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
