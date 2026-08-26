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

import com.codingas.gateway.provider.catalog.ProvisionResult;
import com.codingas.gateway.provider.catalog.PlanCatalog;
import com.codingas.gateway.provider.catalog.CatalogException;
import com.codingas.gateway.provider.catalog.PlanCatalogRepository;
import com.codingas.gateway.provider.catalog.PlanModelCatalogRepository;
import com.codingas.gateway.provider.vendor.Provider;
import com.codingas.gateway.provider.model.BillingMode;
import com.codingas.gateway.provider.model.ModelRepository;
import com.codingas.gateway.provider.model.ModelInstanceRepository;
import com.codingas.gateway.provider.vendor.ProviderRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * ChannelProvisionManager.inlineProvider 入参三路径单元测试
 *
 * <p>覆盖 ensureProvider 在 inlineProvider 不同入参下的行为：</p>
 * <ul>
 *     <li>providerCode 不存在 + inlineProvider 非空：用 inline 字段创建</li>
 *     <li>providerCode 不存在 + inlineProvider 为空：旧默认级联（name=providerCode）</li>
 *     <li>providerCode 已存在：inlineProvider 被忽略，不调用 save</li>
 *     <li>inlineProvider.code 与 planCode 解析出的 providerCode 不一致：抛 INLINE_PROVIDER_CODE_MISMATCH</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ChannelProvisionManager inlineProvider 入参三路径")
class ChannelProvisionManagerInlineProviderTest {

    @Mock
    private PlanCatalogRepository planCatalogRepository;
    @Mock
    private PlanModelCatalogRepository planModelCatalogRepository;
    @Mock
    private ProviderRepository providerRepository;
    @Mock
    private ChannelRepository channelRepository;
    @Mock
    private ChannelEndpointRepository channelEndpointRepository;
    @Mock
    private ModelInstanceRepository modelInstanceRepository;
    @Mock
    private ChannelCredentialRepository channelCredentialRepository;
    @Mock
    private ModelRepository modelRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private ChannelProvisionManager service;

    private static final String PLAN_CODE = "openai-paid";
    private static final String PROVIDER_CODE = "openai";

    @BeforeEach
    void setUp() {
        service = new ChannelProvisionManager(
                planCatalogRepository,
                planModelCatalogRepository,
                providerRepository,
                channelRepository,
                channelEndpointRepository,
                modelInstanceRepository,
                channelCredentialRepository,
                modelRepository,
                objectMapper);
    }

    /**
     * 构建一个最小可用的 PlanCatalog 桩（无 endpoints / 无 pricing，避免触发后续创建逻辑）
     */
    private PlanCatalog stubPlan() {
        PlanCatalog plan = new PlanCatalog();
        plan.setPlanCode(PLAN_CODE);
        plan.setProviderCode(PROVIDER_CODE);
        plan.setBillingMode(BillingMode.PAY_AS_YOU_GO);
        plan.setEndpoints(null);
        plan.setPricing(null);
        return plan;
    }

    /**
     * 模拟 providerRepository.save 返回带 ID 的 Provider
     */
    private void stubProviderSave() {
        when(providerRepository.save(any(Provider.class))).thenAnswer(inv -> {
            Provider p = inv.getArgument(0);
            p.setId(99L);
            return p;
        });
    }

    /**
     * 模拟 channelRepository.save 返回带 ID 的 Channel（避免后续 NPE）
     */
    private void stubChannelSave() {
        when(channelRepository.existsByProviderIdAndName(any(), any())).thenReturn(false);
        when(channelRepository.save(any(Channel.class))).thenAnswer(inv -> {
            Channel c = inv.getArgument(0);
            c.setId(1000L);
            return c;
        });
    }

    @Test
    @DisplayName("providerCode 不存在 + inlineProvider 非空 → 使用 inline 字段创建 Provider")
    void providerCode不存在_inlineProvider非空_使用inline创建Provider() {
        // 准备
        when(planCatalogRepository.findByPlanCode(PLAN_CODE)).thenReturn(Optional.of(stubPlan()));
        when(providerRepository.findByCode(PROVIDER_CODE)).thenReturn(Optional.empty());
        stubProviderSave();
        stubChannelSave();

        ProvisionResult result = service.provisionFromPlan(PLAN_CODE, null, new InlineProviderParams(
                        PROVIDER_CODE,
                        "OpenAI 显示名",
                        "供应商描述",
                        "https://openai.com",
                        "https://platform.openai.com/docs"));

        // 断言：保存 Provider 时使用了 inline 字段
        ArgumentCaptor<Provider> captor = ArgumentCaptor.forClass(Provider.class);
        verify(providerRepository).save(captor.capture());
        Provider saved = captor.getValue();
        assertThat(saved.getCode()).isEqualTo(PROVIDER_CODE);
        assertThat(saved.getName()).isEqualTo("OpenAI 显示名");
        assertThat(saved.getDescription()).isEqualTo("供应商描述");
        assertThat(saved.getWebsiteUrl()).isEqualTo("https://openai.com");
        assertThat(saved.getApiDocUrl()).isEqualTo("https://platform.openai.com/docs");
        assertThat(saved.getPriority()).isEqualTo(100);

        assertThat(result.getStatus()).isEqualTo("CREATED");
    }

    @Test
    @DisplayName("providerCode 不存在 + inlineProvider 为空 → 旧默认级联（name=providerCode）")
    void providerCode不存在_inlineProvider为空_使用旧默认级联创建() {
        when(planCatalogRepository.findByPlanCode(PLAN_CODE)).thenReturn(Optional.of(stubPlan()));
        when(providerRepository.findByCode(PROVIDER_CODE)).thenReturn(Optional.empty());
        stubProviderSave();
        stubChannelSave();

        service.provisionFromPlan(PLAN_CODE, null, null);

        ArgumentCaptor<Provider> captor = ArgumentCaptor.forClass(Provider.class);
        verify(providerRepository).save(captor.capture());
        Provider saved = captor.getValue();
        assertThat(saved.getCode()).isEqualTo(PROVIDER_CODE);
        // 默认行为：name=providerCode，其它扩展字段为 null
        assertThat(saved.getName()).isEqualTo(PROVIDER_CODE);
        assertThat(saved.getDescription()).isNull();
        assertThat(saved.getWebsiteUrl()).isNull();
        assertThat(saved.getApiDocUrl()).isNull();
    }

    @Test
    @DisplayName("providerCode 已存在 → inlineProvider 被忽略，providerRepository.save 永不调用")
    void providerCode已存在_inlineProvider被忽略() {
        Provider existing = new Provider();
        existing.setId(7L);
        existing.setCode(PROVIDER_CODE);
        existing.setName("Existing Provider");

        when(planCatalogRepository.findByPlanCode(PLAN_CODE)).thenReturn(Optional.of(stubPlan()));
        when(providerRepository.findByCode(PROVIDER_CODE)).thenReturn(Optional.of(existing));
        stubChannelSave();

        ProvisionResult result = service.provisionFromPlan(PLAN_CODE, null, new InlineProviderParams(
                        PROVIDER_CODE, "新名字（应被忽略）", null, null, null));

        // 关键断言：现有 Provider 已存在时，永不触发 save
        verify(providerRepository, never()).save(any(Provider.class));
    }

    @Test
    @DisplayName("inlineProvider.code 与 planCode 的 providerCode 不一致 → 抛 INLINE_PROVIDER_CODE_MISMATCH")
    void inlineProvider_code与planCode的providerCode不一致_抛出_INLINE_PROVIDER_CODE_MISMATCH() {
        when(planCatalogRepository.findByPlanCode(PLAN_CODE)).thenReturn(Optional.of(stubPlan()));
        // 该路径在创建任何资源前就抛错，使用 lenient 防止 strict 模式抱怨未使用桩
        lenient().when(providerRepository.findByCode(any())).thenReturn(Optional.empty());

        Assertions.assertThatThrownBy(() -> service.provisionFromPlan(PLAN_CODE, null, new InlineProviderParams(
                        "wrong-code", "Wrong", null, null, null)))
                .isInstanceOf(CatalogException.class)
                .hasMessageContaining("INLINE_PROVIDER_CODE_MISMATCH");

        // 不应触及任何写操作
        verify(providerRepository, never()).save(any(Provider.class));
        verify(channelRepository, never()).save(any(Channel.class));
    }
}
