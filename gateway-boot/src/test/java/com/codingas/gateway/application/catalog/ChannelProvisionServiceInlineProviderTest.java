package com.codingas.gateway.application.catalog;

import com.codingas.gateway.application.catalog.dto.ProvisionRequest;
import com.codingas.gateway.application.catalog.dto.ProvisionResult;
import com.codingas.gateway.domain.supply.catalog.entity.PlanCatalog;
import com.codingas.gateway.domain.supply.catalog.exception.CatalogException;
import com.codingas.gateway.domain.supply.catalog.gateway.PlanCatalogGateway;
import com.codingas.gateway.domain.supply.catalog.gateway.PlanModelCatalogGateway;
import com.codingas.gateway.domain.supply.entity.Channel;
import com.codingas.gateway.domain.supply.entity.Provider;
import com.codingas.gateway.domain.supply.enums.BillingMode;
import com.codingas.gateway.domain.supply.gateway.ChannelCredentialGateway;
import com.codingas.gateway.domain.supply.gateway.ChannelEndpointGateway;
import com.codingas.gateway.domain.supply.gateway.ChannelGateway;
import com.codingas.gateway.domain.supply.gateway.ModelGateway;
import com.codingas.gateway.domain.supply.gateway.ModelInstanceGateway;
import com.codingas.gateway.domain.supply.gateway.ProviderGateway;
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
 * ChannelProvisionService.inlineProvider 入参三路径单元测试
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
@DisplayName("ChannelProvisionService inlineProvider 入参三路径")
class ChannelProvisionServiceInlineProviderTest {

    @Mock
    private PlanCatalogGateway planCatalogGateway;
    @Mock
    private PlanModelCatalogGateway planModelCatalogGateway;
    @Mock
    private ProviderGateway providerGateway;
    @Mock
    private ChannelGateway channelGateway;
    @Mock
    private ChannelEndpointGateway channelEndpointGateway;
    @Mock
    private ModelInstanceGateway modelInstanceGateway;
    @Mock
    private ChannelCredentialGateway channelCredentialGateway;
    @Mock
    private ModelGateway modelGateway;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private ChannelProvisionService service;

    private static final String PLAN_CODE = "openai-paid";
    private static final String PROVIDER_CODE = "openai";

    @BeforeEach
    void setUp() {
        service = new ChannelProvisionService(
                planCatalogGateway,
                planModelCatalogGateway,
                providerGateway,
                channelGateway,
                channelEndpointGateway,
                modelInstanceGateway,
                channelCredentialGateway,
                modelGateway,
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
     * 模拟 providerGateway.save 返回带 ID 的 Provider
     */
    private void stubProviderSave() {
        when(providerGateway.save(any(Provider.class))).thenAnswer(inv -> {
            Provider p = inv.getArgument(0);
            p.setId(99L);
            return p;
        });
    }

    /**
     * 模拟 channelGateway.save 返回带 ID 的 Channel（避免后续 NPE）
     */
    private void stubChannelSave() {
        when(channelGateway.existsByProviderIdAndName(any(), any())).thenReturn(false);
        when(channelGateway.save(any(Channel.class))).thenAnswer(inv -> {
            Channel c = inv.getArgument(0);
            c.setId(1000L);
            return c;
        });
    }

    @Test
    @DisplayName("providerCode 不存在 + inlineProvider 非空 → 使用 inline 字段创建 Provider")
    void providerCode不存在_inlineProvider非空_使用inline创建Provider() {
        // 准备
        when(planCatalogGateway.findByPlanCode(PLAN_CODE)).thenReturn(Optional.of(stubPlan()));
        when(providerGateway.findByCode(PROVIDER_CODE)).thenReturn(Optional.empty());
        stubProviderSave();
        stubChannelSave();

        ProvisionRequest request = new ProvisionRequest();
        request.setInlineProvider(new ProvisionRequest.InlineProvider(
                PROVIDER_CODE,
                "OpenAI 显示名",
                "供应商描述",
                "https://openai.com",
                "https://platform.openai.com/docs"));

        // 执行
        ProvisionResult result = service.provisionFromPlan(PLAN_CODE, request);

        // 断言：保存 Provider 时使用了 inline 字段
        ArgumentCaptor<Provider> captor = ArgumentCaptor.forClass(Provider.class);
        verify(providerGateway).save(captor.capture());
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
        when(planCatalogGateway.findByPlanCode(PLAN_CODE)).thenReturn(Optional.of(stubPlan()));
        when(providerGateway.findByCode(PROVIDER_CODE)).thenReturn(Optional.empty());
        stubProviderSave();
        stubChannelSave();

        ProvisionRequest request = new ProvisionRequest();
        // inlineProvider == null

        service.provisionFromPlan(PLAN_CODE, request);

        ArgumentCaptor<Provider> captor = ArgumentCaptor.forClass(Provider.class);
        verify(providerGateway).save(captor.capture());
        Provider saved = captor.getValue();
        assertThat(saved.getCode()).isEqualTo(PROVIDER_CODE);
        // 默认行为：name=providerCode，其它扩展字段为 null
        assertThat(saved.getName()).isEqualTo(PROVIDER_CODE);
        assertThat(saved.getDescription()).isNull();
        assertThat(saved.getWebsiteUrl()).isNull();
        assertThat(saved.getApiDocUrl()).isNull();
    }

    @Test
    @DisplayName("providerCode 已存在 → inlineProvider 被忽略，providerGateway.save 永不调用")
    void providerCode已存在_inlineProvider被忽略() {
        Provider existing = new Provider();
        existing.setId(7L);
        existing.setCode(PROVIDER_CODE);
        existing.setName("Existing Provider");

        when(planCatalogGateway.findByPlanCode(PLAN_CODE)).thenReturn(Optional.of(stubPlan()));
        when(providerGateway.findByCode(PROVIDER_CODE)).thenReturn(Optional.of(existing));
        stubChannelSave();

        ProvisionRequest request = new ProvisionRequest();
        request.setInlineProvider(new ProvisionRequest.InlineProvider(
                PROVIDER_CODE, "新名字（应被忽略）", null, null, null));

        service.provisionFromPlan(PLAN_CODE, request);

        // 关键断言：现有 Provider 已存在时，永不触发 save
        verify(providerGateway, never()).save(any(Provider.class));
    }

    @Test
    @DisplayName("inlineProvider.code 与 planCode 的 providerCode 不一致 → 抛 INLINE_PROVIDER_CODE_MISMATCH")
    void inlineProvider_code与planCode的providerCode不一致_抛出_INLINE_PROVIDER_CODE_MISMATCH() {
        when(planCatalogGateway.findByPlanCode(PLAN_CODE)).thenReturn(Optional.of(stubPlan()));
        // 该路径在创建任何资源前就抛错，使用 lenient 防止 strict 模式抱怨未使用桩
        lenient().when(providerGateway.findByCode(any())).thenReturn(Optional.empty());

        ProvisionRequest request = new ProvisionRequest();
        request.setInlineProvider(new ProvisionRequest.InlineProvider(
                "wrong-code", "Wrong", null, null, null));

        Assertions.assertThatThrownBy(() -> service.provisionFromPlan(PLAN_CODE, request))
                .isInstanceOf(CatalogException.class)
                .hasMessageContaining("INLINE_PROVIDER_CODE_MISMATCH");

        // 不应触及任何写操作
        verify(providerGateway, never()).save(any(Provider.class));
        verify(channelGateway, never()).save(any(Channel.class));
    }
}
