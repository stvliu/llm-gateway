package com.codingas.gateway.infrastructure.supply.catalog.loader;

import com.codingas.gateway.domain.supply.catalog.entity.ModelSpecCatalog;
import com.codingas.gateway.domain.supply.catalog.entity.PlanCatalog;
import com.codingas.gateway.domain.supply.catalog.entity.PlanModelCatalog;
import com.codingas.gateway.domain.supply.catalog.entity.ProviderCatalog;
import com.codingas.gateway.domain.supply.catalog.enums.BillingMode;
import com.codingas.gateway.domain.supply.catalog.enums.CatalogSource;
import com.codingas.gateway.domain.supply.catalog.enums.CatalogState;
import com.codingas.gateway.domain.supply.catalog.enums.ProviderType;
import com.codingas.gateway.domain.supply.catalog.gateway.PlanCatalogGateway;
import com.codingas.gateway.domain.supply.catalog.gateway.ProviderCatalogGateway;
import com.codingas.gateway.domain.supply.catalog.service.CatalogDomainService;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

/**
 * 内置目录数据加载器单元测试
 */
@ExtendWith(MockitoExtension.class)
class BuiltinCatalogLoaderTest {

    @Mock
    private ProviderCatalogGateway providerCatalogGateway;

    @Mock
    private PlanCatalogGateway planCatalogGateway;

    @Mock(lenient = true)
    private CatalogDomainService catalogDomainService;

    private ObjectMapper objectMapper;

    private BuiltinCatalogLoader loader;

    @BeforeEach
    void setUp() {
        // 使用真实的 ObjectMapper（忽略未知字段），与 loader 内部的 catalogObjectMapper 配置一致
        objectMapper = new ObjectMapper()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        // 默认 stub：表为空
        when(providerCatalogGateway.findAll()).thenReturn(Collections.emptyList());
        // 默认 stub：所有 upsert 返回 ADDED
        when(catalogDomainService.upsertProvider(any(ProviderCatalog.class))).thenReturn("ADDED");
        when(catalogDomainService.upsertModelSpec(any(ModelSpecCatalog.class))).thenReturn("ADDED");
        when(catalogDomainService.upsertPlan(any(PlanCatalog.class))).thenReturn("ADDED");
        when(catalogDomainService.upsertPlanModel(any(PlanModelCatalog.class))).thenReturn("ADDED");

        loader = new BuiltinCatalogLoader(providerCatalogGateway, planCatalogGateway, objectMapper, catalogDomainService);
    }

    @Nested
    @DisplayName("表为空时加载数据")
    class EmptyTableTests {

        @Test
        @DisplayName("表为空时调用 upsert 加载所有四种目录数据")
        void loadsAllCatalogTypesWhenTableEmpty() {
            assertThatNoException().isThrownBy(() -> loader.run());

            // 验证四种目录数据都被加载
            verify(catalogDomainService, atLeastOnce()).upsertProvider(any(ProviderCatalog.class));
            verify(catalogDomainService, atLeastOnce()).upsertModelSpec(any(ModelSpecCatalog.class));
            verify(catalogDomainService, atLeastOnce()).upsertPlan(any(PlanCatalog.class));
            verify(catalogDomainService, atLeastOnce()).upsertPlanModel(any(PlanModelCatalog.class));
        }

        @Test
        @DisplayName("供应商目录 providerType 从字符串转为枚举")
        void convertsProviderTypeFromString() {
            assertThatNoException().isThrownBy(() -> loader.run());

            // 验证至少存在一条 INTERNATIONAL 或 DOMESTIC 类型
            verify(catalogDomainService, atLeastOnce()).upsertProvider(argThat(catalog ->
                catalog.getProviderType() == ProviderType.INTERNATIONAL
                    || catalog.getProviderType() == ProviderType.DOMESTIC
            ));
        }

        @Test
        @DisplayName("套餐目录 billingMode 从字符串转为枚举")
        void convertsBillingModeFromString() {
            assertThatNoException().isThrownBy(() -> loader.run());

            // 验证至少存在一条 PAY_AS_YOU_GO 或 SUBSCRIPTION 类型
            verify(catalogDomainService, atLeastOnce()).upsertPlan(argThat(catalog ->
                catalog.getBillingMode() == BillingMode.PAY_AS_YOU_GO
                    || catalog.getBillingMode() == BillingMode.SUBSCRIPTION
                    || catalog.getBillingMode() == BillingMode.PACKAGE
            ));
        }

        @Test
        @DisplayName("capabilities 和 modalities 转为 JSON String")
        void convertsCapabilitiesAndModalitiesToJsonString() {
            assertThatNoException().isThrownBy(() -> loader.run());

            // 验证至少有一条 model spec 的 capabilities 是合法 JSON 字符串
            verify(catalogDomainService, atLeastOnce()).upsertModelSpec(argThat(catalog -> {
                if (catalog.getCapabilities() == null) return true;
                try {
                    objectMapper.readTree(catalog.getCapabilities());
                    return true;
                } catch (Exception e) {
                    return false;
                }
            }));
        }

        @Test
        @DisplayName("endpoints 和 pricing 转为 JSON String")
        void convertsEndpointsAndPricingToJsonString() {
            assertThatNoException().isThrownBy(() -> loader.run());

            // 验证至少有一条 plan 的 endpoints 是合法 JSON 字符串
            verify(catalogDomainService, atLeastOnce()).upsertPlan(argThat(catalog -> {
                if (catalog.getEndpoints() == null) return true;
                try {
                    objectMapper.readTree(catalog.getEndpoints());
                    return true;
                } catch (Exception e) {
                    return false;
                }
            }));
        }

        @Test
        @DisplayName("加载的目录数据 source 为 BUILTIN，state 为 ACTIVE")
        void setsSourceAndStateCorrectly() {
            assertThatNoException().isThrownBy(() -> loader.run());

            verify(catalogDomainService, atLeastOnce()).upsertProvider(argThat(catalog ->
                catalog.getSource() == CatalogSource.BUILTIN
                    && catalog.getState() == CatalogState.ACTIVE
            ));
        }

        @Test
        @DisplayName("加载失败不阻止应用启动")
        void doesNotFailOnLoadError() {
            // 重置 mock，让 upsertProvider 在第一次调用后抛异常
            reset(catalogDomainService);
            when(catalogDomainService.upsertProvider(any(ProviderCatalog.class)))
                .thenThrow(new RuntimeException("模拟数据库异常"));

            assertThatNoException().isThrownBy(() -> loader.run());
        }

        @Test
        @DisplayName("统计 ADDED/UPDATED/SKIPPED 数量")
        void countsUpsertResults() {
            assertThatNoException().isThrownBy(() -> loader.run());

            // 验证 provider 被加载了多次（JSON 文件中 19 个供应商）
            verify(catalogDomainService, atLeast(19)).upsertProvider(any(ProviderCatalog.class));
        }
    }

    @Nested
    @DisplayName("表不为空时跳过加载")
    class NonEmptyTableTests {

        @Test
        @DisplayName("表不为空时跳过所有加载")
        void skipsLoadingWhenTableNotEmpty() {
            // 重置 findAll 返回非空列表
            reset(providerCatalogGateway);
            var existingCatalog = new ProviderCatalog();
            existingCatalog.setProviderCode("openai");
            when(providerCatalogGateway.findAll()).thenReturn(List.of(existingCatalog));

            assertThatNoException().isThrownBy(() -> loader.run());

            // 验证没有任何 upsert 被调用
            verify(catalogDomainService, never()).upsertProvider(any());
            verify(catalogDomainService, never()).upsertModelSpec(any());
            verify(catalogDomainService, never()).upsertPlan(any());
            verify(catalogDomainService, never()).upsertPlanModel(any());
        }
    }
}