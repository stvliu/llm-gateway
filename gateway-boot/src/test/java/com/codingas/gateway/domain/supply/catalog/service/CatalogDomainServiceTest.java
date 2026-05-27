package com.codingas.gateway.domain.supply.catalog.service;

import com.codingas.gateway.domain.supply.catalog.entity.ModelCatalog;
import com.codingas.gateway.domain.supply.catalog.entity.PlanCatalog;
import com.codingas.gateway.domain.supply.catalog.entity.PlanModelCatalog;
import com.codingas.gateway.domain.supply.catalog.entity.ProviderCatalog;
import com.codingas.gateway.domain.supply.catalog.enums.BillingMode;
import com.codingas.gateway.domain.supply.catalog.enums.CatalogSource;
import com.codingas.gateway.domain.supply.catalog.enums.CatalogState;
import com.codingas.gateway.domain.supply.catalog.enums.ProviderType;
import com.codingas.gateway.domain.supply.catalog.gateway.ModelCatalogGateway;
import com.codingas.gateway.domain.supply.catalog.gateway.PlanCatalogGateway;
import com.codingas.gateway.domain.supply.catalog.gateway.PlanModelCatalogGateway;
import com.codingas.gateway.domain.supply.catalog.gateway.ProviderCatalogGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * CatalogDomainService 测试
 *
 * <p>覆盖 upsert（ADDED / UPDATED / SKIPPED）和 markDeprecated 核心逻辑。</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CatalogDomainService 测试")
class CatalogDomainServiceTest {

    @Mock
    private ProviderCatalogGateway providerCatalogGateway;

    @Mock
    private PlanCatalogGateway planCatalogGateway;

    @Mock
    private PlanModelCatalogGateway planModelCatalogGateway;

    @Mock
    private ModelCatalogGateway modelCatalogGateway;

    private CatalogDomainService service;

    @BeforeEach
    void setUp() {
        service = new CatalogDomainService(
                providerCatalogGateway,
                planCatalogGateway,
                planModelCatalogGateway,
                modelCatalogGateway
        );
    }

    // ===== upsertProvider =====

    @Nested
    @DisplayName("upsertProvider 测试")
    class UpsertProviderTests {

        @Test
        @DisplayName("不存在时新增，返回 ADDED")
        void upsertProvider_notExists_returnsAdded() {
            ProviderCatalog catalog = buildProviderCatalog("openai", CatalogSource.BUILTIN);

            when(providerCatalogGateway.findByProviderCode("openai")).thenReturn(Optional.empty());
            when(providerCatalogGateway.save(any(ProviderCatalog.class))).thenAnswer(inv -> inv.getArgument(0));

            String result = service.upsertProvider(catalog);

            assertThat(result).isEqualTo("ADDED");
            verify(providerCatalogGateway).save(catalog);
        }

        @Test
        @DisplayName("同优先级来源可覆盖，返回 UPDATED")
        void upsertProvider_samePriority_canOverride_returnsUpdated() {
            ProviderCatalog incoming = buildProviderCatalog("openai", CatalogSource.BUILTIN);
            incoming.setProviderName("OpenAI Updated");

            ProviderCatalog existing = buildProviderCatalog("openai", CatalogSource.BUILTIN);
            existing.setProviderName("OpenAI Old");

            when(providerCatalogGateway.findByProviderCode("openai")).thenReturn(Optional.of(existing));
            when(providerCatalogGateway.save(any(ProviderCatalog.class))).thenAnswer(inv -> inv.getArgument(0));

            String result = service.upsertProvider(incoming);

            assertThat(result).isEqualTo("UPDATED");
            verify(providerCatalogGateway).save(existing);
            // 验证字段被拷贝
            assertThat(existing.getProviderName()).isEqualTo("OpenAI Updated");
        }

        @Test
        @DisplayName("高优先级来源可覆盖低优先级，返回 UPDATED")
        void upsertProvider_higherPriority_canOverride_returnsUpdated() {
            ProviderCatalog incoming = buildProviderCatalog("openai", CatalogSource.MANUAL);
            ProviderCatalog existing = buildProviderCatalog("openai", CatalogSource.BUILTIN);

            when(providerCatalogGateway.findByProviderCode("openai")).thenReturn(Optional.of(existing));
            when(providerCatalogGateway.save(any(ProviderCatalog.class))).thenAnswer(inv -> inv.getArgument(0));

            String result = service.upsertProvider(incoming);

            assertThat(result).isEqualTo("UPDATED");
            assertThat(existing.getSource()).isEqualTo(CatalogSource.MANUAL);
        }

        @Test
        @DisplayName("低优先级来源不可覆盖高优先级，返回 SKIPPED")
        void upsertProvider_lowerPriority_cannotOverride_returnsSkipped() {
            ProviderCatalog incoming = buildProviderCatalog("openai", CatalogSource.BUILTIN);
            ProviderCatalog existing = buildProviderCatalog("openai", CatalogSource.MANUAL);

            when(providerCatalogGateway.findByProviderCode("openai")).thenReturn(Optional.of(existing));

            String result = service.upsertProvider(incoming);

            assertThat(result).isEqualTo("SKIPPED");
            verify(providerCatalogGateway, never()).save(any());
        }
    }

    // ===== upsertPlan =====

    @Nested
    @DisplayName("upsertPlan 测试")
    class UpsertPlanTests {

        @Test
        @DisplayName("不存在时新增，返回 ADDED")
        void upsertPlan_notExists_returnsAdded() {
            PlanCatalog catalog = buildPlanCatalog("openai_payg", CatalogSource.BUILTIN);

            when(planCatalogGateway.findByPlanCode("openai_payg")).thenReturn(Optional.empty());
            when(planCatalogGateway.save(any(PlanCatalog.class))).thenAnswer(inv -> inv.getArgument(0));

            String result = service.upsertPlan(catalog);

            assertThat(result).isEqualTo("ADDED");
            verify(planCatalogGateway).save(catalog);
        }

        @Test
        @DisplayName("高优先级可覆盖，返回 UPDATED")
        void upsertPlan_higherPriority_returnsUpdated() {
            PlanCatalog incoming = buildPlanCatalog("openai_payg", CatalogSource.PROVIDER_API);
            PlanCatalog existing = buildPlanCatalog("openai_payg", CatalogSource.BUILTIN);

            when(planCatalogGateway.findByPlanCode("openai_payg")).thenReturn(Optional.of(existing));
            when(planCatalogGateway.save(any(PlanCatalog.class))).thenAnswer(inv -> inv.getArgument(0));

            String result = service.upsertPlan(incoming);

            assertThat(result).isEqualTo("UPDATED");
            assertThat(existing.getSource()).isEqualTo(CatalogSource.PROVIDER_API);
        }

        @Test
        @DisplayName("低优先级不可覆盖，返回 SKIPPED")
        void upsertPlan_lowerPriority_returnsSkipped() {
            PlanCatalog incoming = buildPlanCatalog("openai_payg", CatalogSource.BUILTIN);
            PlanCatalog existing = buildPlanCatalog("openai_payg", CatalogSource.MANUAL);

            when(planCatalogGateway.findByPlanCode("openai_payg")).thenReturn(Optional.of(existing));

            String result = service.upsertPlan(incoming);

            assertThat(result).isEqualTo("SKIPPED");
            verify(planCatalogGateway, never()).save(any());
        }
    }

    // ===== upsertPlanModel =====

    @Nested
    @DisplayName("upsertPlanModel 测试")
    class UpsertPlanModelTests {

        @Test
        @DisplayName("不存在时新增，返回 ADDED")
        void upsertPlanModel_notExists_returnsAdded() {
            PlanModelCatalog catalog = buildPlanModelCatalog("openai_payg", "gpt-4o", CatalogSource.BUILTIN);

            when(planModelCatalogGateway.findByPlanCodeAndModelName("openai_payg", "gpt-4o"))
                    .thenReturn(Optional.empty());
            when(planModelCatalogGateway.save(any(PlanModelCatalog.class))).thenAnswer(inv -> inv.getArgument(0));

            String result = service.upsertPlanModel(catalog);

            assertThat(result).isEqualTo("ADDED");
            verify(planModelCatalogGateway).save(catalog);
        }

        @Test
        @DisplayName("同优先级可覆盖，返回 UPDATED")
        void upsertPlanModel_samePriority_returnsUpdated() {
            PlanModelCatalog incoming = buildPlanModelCatalog("openai_payg", "gpt-4o", CatalogSource.BUILTIN);
            PlanModelCatalog existing = buildPlanModelCatalog("openai_payg", "gpt-4o", CatalogSource.BUILTIN);

            when(planModelCatalogGateway.findByPlanCodeAndModelName("openai_payg", "gpt-4o"))
                    .thenReturn(Optional.of(existing));
            when(planModelCatalogGateway.save(any(PlanModelCatalog.class))).thenAnswer(inv -> inv.getArgument(0));

            String result = service.upsertPlanModel(incoming);

            assertThat(result).isEqualTo("UPDATED");
            verify(planModelCatalogGateway).save(existing);
        }

        @Test
        @DisplayName("低优先级不可覆盖，返回 SKIPPED")
        void upsertPlanModel_lowerPriority_returnsSkipped() {
            PlanModelCatalog incoming = buildPlanModelCatalog("openai_payg", "gpt-4o", CatalogSource.BUILTIN);
            PlanModelCatalog existing = buildPlanModelCatalog("openai_payg", "gpt-4o", CatalogSource.PROVIDER_API);

            when(planModelCatalogGateway.findByPlanCodeAndModelName("openai_payg", "gpt-4o"))
                    .thenReturn(Optional.of(existing));

            String result = service.upsertPlanModel(incoming);

            assertThat(result).isEqualTo("SKIPPED");
            verify(planModelCatalogGateway, never()).save(any());
        }
    }

    // ===== upsertModel =====

    @Nested
    @DisplayName("upsertModel 测试")
    class UpsertModelTests {

        @Test
        @DisplayName("不存在时新增，返回 ADDED")
        void upsertModel_notExists_returnsAdded() {
            ModelCatalog catalog = buildModelCatalog("gpt-4o", CatalogSource.BUILTIN);

            when(modelCatalogGateway.findByModelName("gpt-4o")).thenReturn(Optional.empty());
            when(modelCatalogGateway.save(any(ModelCatalog.class))).thenAnswer(inv -> inv.getArgument(0));

            String result = service.upsertModel(catalog);

            assertThat(result).isEqualTo("ADDED");
            verify(modelCatalogGateway).save(catalog);
        }

        @Test
        @DisplayName("高优先级可覆盖，返回 UPDATED，字段被拷贝")
        void upsertModel_higherPriority_returnsUpdated() {
            ModelCatalog incoming = buildModelCatalog("gpt-4o", CatalogSource.PROVIDER_API);
            incoming.setDisplayName("GPT-4o Updated");

            ModelCatalog existing = buildModelCatalog("gpt-4o", CatalogSource.BUILTIN);
            existing.setDisplayName("GPT-4o Old");

            when(modelCatalogGateway.findByModelName("gpt-4o")).thenReturn(Optional.of(existing));
            when(modelCatalogGateway.save(any(ModelCatalog.class))).thenAnswer(inv -> inv.getArgument(0));

            String result = service.upsertModel(incoming);

            assertThat(result).isEqualTo("UPDATED");
            assertThat(existing.getDisplayName()).isEqualTo("GPT-4o Updated");
            assertThat(existing.getSource()).isEqualTo(CatalogSource.PROVIDER_API);
        }

        @Test
        @DisplayName("低优先级不可覆盖，返回 SKIPPED")
        void upsertModel_lowerPriority_returnsSkipped() {
            ModelCatalog incoming = buildModelCatalog("gpt-4o", CatalogSource.BUILTIN);
            ModelCatalog existing = buildModelCatalog("gpt-4o", CatalogSource.MANUAL);

            when(modelCatalogGateway.findByModelName("gpt-4o")).thenReturn(Optional.of(existing));

            String result = service.upsertModel(incoming);

            assertThat(result).isEqualTo("SKIPPED");
            verify(modelCatalogGateway, never()).save(any());
        }
    }

    // ===== markProvidersDeprecated =====

    @Nested
    @DisplayName("markProvidersDeprecated 测试")
    class MarkProvidersDeprecatedTests {

        @Test
        @DisplayName("未出现的条目标记为 DEPRECATED")
        void markProvidersDeprecated_deprecatesMissingEntries() {
            ProviderCatalog activeProvider = buildProviderCatalog("openai", CatalogSource.BUILTIN);
            activeProvider.setState(CatalogState.ACTIVE);

            ProviderCatalog removedProvider = buildProviderCatalog("deepseek", CatalogSource.BUILTIN);
            removedProvider.setState(CatalogState.ACTIVE);

            // activeCodes 中只有 "openai"，"deepseek" 不在其中，应被标记
            when(providerCatalogGateway.findBySourceExcludingKeys(CatalogSource.BUILTIN, List.of("openai")))
                    .thenReturn(List.of(removedProvider));
            when(providerCatalogGateway.save(any(ProviderCatalog.class))).thenAnswer(inv -> inv.getArgument(0));

            service.markProvidersDeprecated(CatalogSource.BUILTIN, List.of("openai"));

            assertThat(removedProvider.getState()).isEqualTo(CatalogState.DEPRECATED);
            verify(providerCatalogGateway).save(removedProvider);
            // activeProvider 不在排除结果中，不会被操作
            verify(providerCatalogGateway, times(1)).save(any());
        }

        @Test
        @DisplayName("已出现的条目保持 ACTIVE")
        void markProvidersDeprecated_keepsActiveEntries() {
            // 没有需要标记的条目
            when(providerCatalogGateway.findBySourceExcludingKeys(CatalogSource.BUILTIN, List.of("openai", "deepseek")))
                    .thenReturn(List.of());

            service.markProvidersDeprecated(CatalogSource.BUILTIN, List.of("openai", "deepseek"));

            verify(providerCatalogGateway, never()).save(any());
        }

        @Test
        @DisplayName("已是 DEPRECATED 状态的条目不会被重复保存")
        void markProvidersDeprecated_alreadyDeprecated_notSavedAgain() {
            ProviderCatalog alreadyDeprecated = buildProviderCatalog("deepseek", CatalogSource.BUILTIN);
            alreadyDeprecated.setState(CatalogState.DEPRECATED);

            when(providerCatalogGateway.findBySourceExcludingKeys(CatalogSource.BUILTIN, List.of("openai")))
                    .thenReturn(List.of(alreadyDeprecated));

            service.markProvidersDeprecated(CatalogSource.BUILTIN, List.of("openai"));

            // 状态未变，不应调用 save
            verify(providerCatalogGateway, never()).save(any());
        }
    }

    // ===== markPlansDeprecated =====

    @Nested
    @DisplayName("markPlansDeprecated 测试")
    class MarkPlansDeprecatedTests {

        @Test
        @DisplayName("未出现的条目标记为 DEPRECATED")
        void markPlansDeprecated_deprecatesMissingEntries() {
            PlanCatalog removedPlan = buildPlanCatalog("old_plan", CatalogSource.BUILTIN);
            removedPlan.setState(CatalogState.ACTIVE);

            when(planCatalogGateway.findBySourceExcludingKeys(CatalogSource.BUILTIN, List.of("active_plan")))
                    .thenReturn(List.of(removedPlan));
            when(planCatalogGateway.save(any(PlanCatalog.class))).thenAnswer(inv -> inv.getArgument(0));

            service.markPlansDeprecated(CatalogSource.BUILTIN, List.of("active_plan"));

            assertThat(removedPlan.getState()).isEqualTo(CatalogState.DEPRECATED);
            verify(planCatalogGateway).save(removedPlan);
        }
    }

    // ===== markModelsDeprecated =====

    @Nested
    @DisplayName("markModelsDeprecated 测试")
    class MarkModelsDeprecatedTests {

        @Test
        @DisplayName("未出现的条目标记为 DEPRECATED")
        void markModelsDeprecated_deprecatesMissingEntries() {
            ModelCatalog removedSpec = buildModelCatalog("old-model", CatalogSource.BUILTIN);
            removedSpec.setState(CatalogState.ACTIVE);

            when(modelCatalogGateway.findBySourceExcludingKeys(CatalogSource.BUILTIN, List.of("active-model")))
                    .thenReturn(List.of(removedSpec));
            when(modelCatalogGateway.save(any(ModelCatalog.class))).thenAnswer(inv -> inv.getArgument(0));

            service.markModelsDeprecated(CatalogSource.BUILTIN, List.of("active-model"));

            assertThat(removedSpec.getState()).isEqualTo(CatalogState.DEPRECATED);
            verify(modelCatalogGateway).save(removedSpec);
        }
    }

    // ===== markPlanModelsDeprecated =====

    @Nested
    @DisplayName("markPlanModelsDeprecated 测试")
    class MarkPlanModelsDeprecatedTests {

        @Test
        @DisplayName("未出现的条目标记为 DEPRECATED")
        void markPlanModelsDeprecated_deprecatesMissingEntries() {
            PlanModelCatalog removedPm = buildPlanModelCatalog("old_plan", "old-model", CatalogSource.BUILTIN);
            removedPm.setState(CatalogState.ACTIVE);

            when(planModelCatalogGateway.findBySourceExcludingKeys(
                    CatalogSource.BUILTIN, List.of("active_plan"), List.of("active-model")))
                    .thenReturn(List.of(removedPm));
            when(planModelCatalogGateway.save(any(PlanModelCatalog.class))).thenAnswer(inv -> inv.getArgument(0));

            service.markPlanModelsDeprecated(CatalogSource.BUILTIN, List.of("active_plan"), List.of("active-model"));

            assertThat(removedPm.getState()).isEqualTo(CatalogState.DEPRECATED);
            verify(planModelCatalogGateway).save(removedPm);
        }

        @Test
        @DisplayName("已是 DEPRECATED 状态的条目不会被重复保存")
        void markPlanModelsDeprecated_alreadyDeprecated_notSavedAgain() {
            PlanModelCatalog alreadyDeprecated = buildPlanModelCatalog("old_plan", "old-model", CatalogSource.BUILTIN);
            alreadyDeprecated.setState(CatalogState.DEPRECATED);

            when(planModelCatalogGateway.findBySourceExcludingKeys(
                    CatalogSource.BUILTIN, List.of("active_plan"), List.of("active-model")))
                    .thenReturn(List.of(alreadyDeprecated));

            service.markPlanModelsDeprecated(CatalogSource.BUILTIN, List.of("active_plan"), List.of("active-model"));

            verify(planModelCatalogGateway, never()).save(any());
        }
    }

    // ===== 辅助构建方法 =====

    private static ProviderCatalog buildProviderCatalog(String providerCode, CatalogSource source) {
        ProviderCatalog catalog = new ProviderCatalog();
        catalog.setProviderCode(providerCode);
        catalog.setProviderName(providerCode + "-name");
        catalog.setProviderType(ProviderType.INTERNATIONAL);
        catalog.setSource(source);
        catalog.setState(CatalogState.ACTIVE);
        catalog.setSyncedAt(Instant.now());
        return catalog;
    }

    private static PlanCatalog buildPlanCatalog(String planCode, CatalogSource source) {
        PlanCatalog catalog = new PlanCatalog();
        catalog.setPlanCode(planCode);
        catalog.setProviderCode("openai");
        catalog.setPlanName(planCode + "-name");
        catalog.setBillingMode(BillingMode.PAY_AS_YOU_GO);
        catalog.setSource(source);
        catalog.setState(CatalogState.ACTIVE);
        catalog.setSyncedAt(Instant.now());
        return catalog;
    }

    private static PlanModelCatalog buildPlanModelCatalog(String planCode, String providerModelId, CatalogSource source) {
        PlanModelCatalog catalog = new PlanModelCatalog();
        catalog.setPlanCode(planCode);
        catalog.setModelName(providerModelId);
        catalog.setSource(source);
        catalog.setState(CatalogState.ACTIVE);
        catalog.setSyncedAt(Instant.now());
        return catalog;
    }

    private static ModelCatalog buildModelCatalog(String providerModelId, CatalogSource source) {
        ModelCatalog catalog = new ModelCatalog();
        catalog.setModelName(providerModelId);
        catalog.setDisplayName(providerModelId + "-display");
        catalog.setModelFamily("gpt");
        catalog.setContextWindow(128000);
        catalog.setMaxInputTokens(128000);
        catalog.setMaxOutputTokens(4096);
        catalog.setSource(source);
        catalog.setState(CatalogState.ACTIVE);
        catalog.setSyncedAt(Instant.now());
        return catalog;
    }
}
