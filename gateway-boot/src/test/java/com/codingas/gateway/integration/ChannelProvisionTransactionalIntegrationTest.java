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
import com.codingas.gateway.provider.channel.ChannelProvisionManager;
import com.codingas.gateway.provider.channel.ProvisionCommand;
import com.codingas.gateway.provider.catalog.ProvisionResult;
import com.codingas.gateway.provider.catalog.PlanCatalog;
import com.codingas.gateway.provider.catalog.PlanCatalogRepository;
import com.codingas.gateway.provider.channel.ChannelEndpoint;
import com.codingas.gateway.provider.vendor.Provider;
import com.codingas.gateway.provider.model.BillingMode;
import com.codingas.gateway.provider.channel.ChannelEndpointRepository;
import com.codingas.gateway.provider.vendor.ProviderRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * ChannelProvisionManager 事务回滚集成测试
 *
 * <p>验证 inlineProvider 内联创建 Provider 与开通 Channel 处于同一事务边界：</p>
 * <ul>
 *     <li>下游写入失败时整体回滚，不留孤儿 Provider</li>
 *     <li>providerCode 已存在时 inlineProvider 被忽略，正常路径仍能走通（不写入 Provider）</li>
 * </ul>
 *
 * <p>关键技术点：</p>
 * <ul>
 *     <li>使用 {@link Propagation#NOT_SUPPORTED} 让外层不携带事务，service 内 {@code @Transactional} 真实生效</li>
 *     <li>{@link MockBean} 替换 {@link ChannelEndpointRepository}，模拟下游写入失败</li>
 *     <li>{@link DirtiesContext} 在每个测试后重置 Spring 上下文，避免被污染的 mock bean 影响其它套件</li>
 * </ul>
 */
@SpringBootTest(classes = GatewayApplication.class)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@DisplayName("ChannelProvisionManager 事务回滚 IT")
class ChannelProvisionTransactionalIntegrationTest {

    @Autowired
    private ChannelProvisionManager service;

    @Autowired
    private ProviderRepository providerRepository;

    @Autowired
    private PlanCatalogRepository planCatalogRepository;

    /** 模拟下游端点写入失败的关键 mock bean */
    @MockBean
    private ChannelEndpointRepository channelEndpointRepository;

    private static final String NEW_PROVIDER_CODE = "brand-new-provider";
    private static final String NEW_PLAN_CODE = "brand-new-plan-rollback";

    private static final String EXISTING_PROVIDER_CODE = "existing-provider-it";
    private static final String EXISTING_PLAN_CODE = "existing-plan-it";

    /**
     * 准备一条 PlanCatalog，便于在 service 中通过 planCode 查询
     */
    private PlanCatalog persistPlan(String planCode, String providerCode) {
        PlanCatalog plan = new PlanCatalog();
        plan.setPlanCode(planCode);
        plan.setProviderCode(providerCode);
        plan.setPlanName(planCode);
        plan.setBillingMode(BillingMode.PAY_AS_YOU_GO);
        // endpoints JSON 必须包含一条记录，确保 service 触发 channelEndpointRepository.save（mock 抛错路径）
        plan.setEndpoints("[{\"protocol\":\"OPENAI\",\"url\":\"https://example.com/v1\"}]");
        plan.setPricing(null);
        return planCatalogRepository.save(plan);
    }

    @BeforeEach
    void setUp() {
        // 每个用例前清理可能残留的 provider/plan，确保独立性
        cleanupArtifacts();
    }

    @AfterEach
    void tearDown() {
        cleanupArtifacts();
    }

    private void cleanupArtifacts() {
        providerRepository.findByCode(NEW_PROVIDER_CODE)
                .ifPresent(providerRepository::delete);
        providerRepository.findByCode(EXISTING_PROVIDER_CODE)
                .ifPresent(providerRepository::delete);
    }

    /**
     * 关键场景：内联创建过程中端点保存失败 → 整体回滚，Provider 未持久化
     */
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @DisplayName("内联创建过程中端点保存失败 → 整体回滚不留孤儿 Provider")
    void 内联创建过程中端点保存失败_整体回滚不留孤儿_Provider() {
        // 准备 PlanCatalog
        persistPlan(NEW_PLAN_CODE, NEW_PROVIDER_CODE);
        // 确保 Provider 此时不存在
        assertThat(providerRepository.findByCode(NEW_PROVIDER_CODE)).isEmpty();

        // mock 端点保存抛异常
        when(channelEndpointRepository.save(any(ChannelEndpoint.class)))
                .thenThrow(new RuntimeException("simulated endpoint save failure"));

        ProvisionCommand request = new ProvisionCommand(null,
                new ProvisionCommand.InlineProviderCommand(
                        NEW_PROVIDER_CODE, "Brand New", "测试用", null, null));

        // 行为：抛错
        assertThatThrownBy(() -> service.provisionFromPlan(NEW_PLAN_CODE, request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("simulated endpoint save failure");

        // 关键断言：事务回滚后，Provider 不应被持久化
        Optional<Provider> orphan = providerRepository.findByCode(NEW_PROVIDER_CODE);
        assertThat(orphan)
                .as("内联创建的 Provider 应随事务回滚而不被持久化")
                .isEmpty();
    }

    /**
     * 关键场景：providerCode 已存在 → inlineProvider 被忽略，正常路径走通（不抛错、不重复写 Provider）
     *
     * <p>本用例不让端点保存抛错（使用默认 mock 返回 null），确保走完 service 主流程到 channelEndpointRepository.save 之前。</p>
     */
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @DisplayName("providerCode 已存在 → inlineProvider 被忽略，正常路径不写 Provider")
    void providerCode已存在时inlineProvider被忽略_正常路径() {
        // 预置一个已存在的 Provider
        Provider preset = new Provider();
        preset.setCode(EXISTING_PROVIDER_CODE);
        preset.setName("Original Existing");
        preset.setPriority(50);
        Provider savedExisting = providerRepository.save(preset);
        assertThat(savedExisting.getId()).isNotNull();

        // 准备 PlanCatalog
        persistPlan(EXISTING_PLAN_CODE, EXISTING_PROVIDER_CODE);

        // mock 端点保存正常返回（避免 NPE）
        when(channelEndpointRepository.save(any(ChannelEndpoint.class)))
                .thenAnswer(inv -> {
                    ChannelEndpoint ep = inv.getArgument(0);
                    ep.setId(System.currentTimeMillis());
                    return ep;
                });

        ProvisionCommand request = new ProvisionCommand(null,
                new ProvisionCommand.InlineProviderCommand(
                        EXISTING_PROVIDER_CODE, "Should-Be-Ignored Name", "应被忽略", null, null));

        // 走通正常路径
        ProvisionResult result = service.provisionFromPlan(EXISTING_PLAN_CODE, request);
        assertThat(result.getStatus()).isEqualTo("CREATED");

        // 关键断言：原有 Provider 的 name 未被 inline 覆盖（走的是已存在分支，没有 save 路径）
        Provider after = providerRepository.findByCode(EXISTING_PROVIDER_CODE).orElseThrow();
        assertThat(after.getName()).isEqualTo("Original Existing");
        assertThat(after.getId()).isEqualTo(savedExisting.getId());
    }
}
