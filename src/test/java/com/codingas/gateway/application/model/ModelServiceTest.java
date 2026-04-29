package com.codingas.gateway.application.model;

import com.codingas.gateway.adapter.admin.dto.model.ModelCreateRequest;
import com.codingas.gateway.adapter.admin.dto.model.ModelQueryRequest;
import com.codingas.gateway.adapter.admin.dto.model.ModelResponse;
import com.codingas.gateway.adapter.admin.dto.model.ModelUpdateRequest;
import com.codingas.gateway.common.dto.PageResponse;
import com.codingas.gateway.common.exception.DuplicateResourceException;
import com.codingas.gateway.common.exception.ResourceNotFoundException;
import com.codingas.gateway.domain.router.entity.Model;
import com.codingas.gateway.domain.router.entity.Model.ModelStatus;
import com.codingas.gateway.domain.router.entity.Provider;
import com.codingas.gateway.domain.router.gateway.ModelGateway;
import com.codingas.gateway.domain.router.gateway.ProviderGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * ModelService 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ModelService 单元测试")
class ModelServiceTest {

    @Mock
    private ModelGateway modelGateway;

    @Mock
    private ProviderGateway providerGateway;

    @InjectMocks
    private ModelServiceImpl modelService;

    private Model testModel;
    private Provider testProvider;

    @BeforeEach
    void setUp() {
        testProvider = createTestProvider(1L, "OPENAI", "OpenAI", "openai");
        testModel = createTestModel(1L, "gpt-4", testProvider, "GPT-4", ModelStatus.ACTIVE);
    }

    // ==================== create 测试 ====================

    @Nested
    @DisplayName("create 创建模型")
    class CreateTests {

        @Test
        @DisplayName("创建模型成功")
        void create_validRequest_returnsModelResponse() {
            // given
            ModelCreateRequest request = new ModelCreateRequest();
            request.setModelCode("gpt-4o");
            request.setProviderId(1L);
            request.setProviderModelId("gpt-4o-2024-08-06");
            request.setDisplayName("GPT-4o");
            request.setContextWindow(128000);
            request.setInputPrice(new BigDecimal("0.000005"));
            request.setOutputPrice(new BigDecimal("0.000015"));

            when(modelGateway.existsByModelCode("gpt-4o")).thenReturn(false);
            when(providerGateway.findById(1L)).thenReturn(Optional.of(testProvider));
            when(modelGateway.save(any(Model.class))).thenAnswer(invocation -> {
                Model model = invocation.getArgument(0);
                model.setId(2L);
                return model;
            });

            // when
            ModelResponse response = modelService.create(request);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(2L);
            assertThat(response.getModelCode()).isEqualTo("gpt-4o");
            assertThat(response.getProviderId()).isEqualTo(1L);
            assertThat(response.getProviderModelId()).isEqualTo("gpt-4o-2024-08-06");
            assertThat(response.getDisplayName()).isEqualTo("GPT-4o");
            assertThat(response.getContextWindow()).isEqualTo(128000);
            assertThat(response.getInputPrice()).isEqualTo(new BigDecimal("0.000005"));
            assertThat(response.getOutputPrice()).isEqualTo(new BigDecimal("0.000015"));
            assertThat(response.getStatus()).isEqualTo(ModelStatus.ACTIVE);
            assertThat(response.getEnabled()).isTrue();

            verify(modelGateway).existsByModelCode("gpt-4o");
            verify(providerGateway).findById(1L);
            verify(modelGateway).save(any(Model.class));
        }

        @Test
        @DisplayName("模型代码重复时抛出 DuplicateResourceException")
        void create_duplicateModelCode_throwsException() {
            // given
            ModelCreateRequest request = new ModelCreateRequest();
            request.setModelCode("gpt-4");
            request.setProviderId(1L);
            request.setProviderModelId("gpt-4-0613");

            when(modelGateway.existsByModelCode("gpt-4")).thenReturn(true);

            // when & then
            assertThatThrownBy(() -> modelService.create(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Model")
                .hasMessageContaining("modelCode");

            verify(modelGateway, never()).save(any(Model.class));
        }

        @Test
        @DisplayName("提供商不存在时抛出 ResourceNotFoundException")
        void create_providerNotFound_throwsException() {
            // given
            ModelCreateRequest request = new ModelCreateRequest();
            request.setModelCode("gpt-4o");
            request.setProviderId(99L);
            request.setProviderModelId("gpt-4o-2024-08-06");

            when(modelGateway.existsByModelCode("gpt-4o")).thenReturn(false);
            when(providerGateway.findById(99L)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> modelService.create(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Provider")
                .hasMessageContaining("99");

            verify(modelGateway, never()).save(any(Model.class));
        }
    }

    // ==================== getById 测试 ====================

    @Nested
    @DisplayName("getById 获取模型")
    class GetByIdTests {

        @Test
        @DisplayName("模型存在时返回模型响应")
        void getById_existingModel_returnsModelResponse() {
            // given
            when(modelGateway.findById(1L)).thenReturn(Optional.of(testModel));

            // when
            ModelResponse response = modelService.getById(1L);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(1L);
            assertThat(response.getModelCode()).isEqualTo("gpt-4");
            assertThat(response.getProviderId()).isEqualTo(1L);
            assertThat(response.getProviderName()).isEqualTo("OpenAI");
            assertThat(response.getProviderCode()).isEqualTo("openai");
            assertThat(response.getDisplayName()).isEqualTo("GPT-4");
            assertThat(response.getStatus()).isEqualTo(ModelStatus.ACTIVE);
            assertThat(response.getEnabled()).isTrue();

            verify(modelGateway).findById(1L);
        }

        @Test
        @DisplayName("模型不存在时抛出 ResourceNotFoundException")
        void getById_nonExistingModel_throwsException() {
            // given
            when(modelGateway.findById(99L)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> modelService.getById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Model")
                .hasMessageContaining("99");

            verify(modelGateway).findById(99L);
        }
    }

    // ==================== query 测试 ====================

    @Nested
    @DisplayName("query 查询模型列表")
    class QueryTests {

        @Test
        @DisplayName("无过滤条件时返回所有模型")
        void query_noFilter_returnsAllModels() {
            // given
            Provider provider2 = createTestProvider(2L, "ANTHROPIC", "Anthropic", "anthropic");
            Model model2 = createTestModel(2L, "claude-3-opus", provider2, "Claude 3 Opus", ModelStatus.ACTIVE);

            when(modelGateway.findAll()).thenReturn(List.of(testModel, model2));

            ModelQueryRequest request = new ModelQueryRequest();
            request.setPage(1);
            request.setLimit(20);

            // when
            PageResponse<ModelResponse> response = modelService.query(request);

            // then
            assertThat(response.getItems()).hasSize(2);
            assertThat(response.getPagination().getTotal()).isEqualTo(2);
            assertThat(response.getPagination().getPage()).isEqualTo(1);
            assertThat(response.getPagination().getLimit()).isEqualTo(20);
        }

        @Test
        @DisplayName("关键字过滤 - 匹配 modelCode")
        void query_withKeyword_matchesModelCode() {
            // given
            when(modelGateway.findAll()).thenReturn(List.of(testModel));

            ModelQueryRequest request = new ModelQueryRequest();
            request.setKeyword("gpt");
            request.setPage(1);
            request.setLimit(20);

            // when
            PageResponse<ModelResponse> response = modelService.query(request);

            // then
            assertThat(response.getItems()).hasSize(1);
            assertThat(response.getItems().get(0).getModelCode()).isEqualTo("gpt-4");
        }

        @Test
        @DisplayName("关键字过滤 - 匹配 displayName")
        void query_withKeyword_matchesDisplayName() {
            // given
            when(modelGateway.findAll()).thenReturn(List.of(testModel));

            ModelQueryRequest request = new ModelQueryRequest();
            request.setKeyword("GPT");
            request.setPage(1);
            request.setLimit(20);

            // when
            PageResponse<ModelResponse> response = modelService.query(request);

            // then
            assertThat(response.getItems()).hasSize(1);
            assertThat(response.getItems().get(0).getDisplayName()).isEqualTo("GPT-4");
        }

        @Test
        @DisplayName("关键字过滤 - 匹配 providerModelId")
        void query_withKeyword_matchesProviderModelId() {
            // given
            when(modelGateway.findAll()).thenReturn(List.of(testModel));

            ModelQueryRequest request = new ModelQueryRequest();
            request.setKeyword("0613");
            request.setPage(1);
            request.setLimit(20);

            // when
            PageResponse<ModelResponse> response = modelService.query(request);

            // then
            assertThat(response.getItems()).hasSize(1);
            assertThat(response.getItems().get(0).getProviderModelId()).contains("0613");
        }

        @Test
        @DisplayName("关键字过滤 - 不匹配时返回空列表")
        void query_withKeyword_noMatch_returnsEmptyList() {
            // given
            when(modelGateway.findAll()).thenReturn(List.of(testModel));

            ModelQueryRequest request = new ModelQueryRequest();
            request.setKeyword("claude");
            request.setPage(1);
            request.setLimit(20);

            // when
            PageResponse<ModelResponse> response = modelService.query(request);

            // then
            assertThat(response.getItems()).isEmpty();
            assertThat(response.getPagination().getTotal()).isEqualTo(0);
        }

        @Test
        @DisplayName("按提供商 ID 过滤")
        void query_withProviderId_filtersModels() {
            // given
            Provider provider2 = createTestProvider(2L, "ANTHROPIC", "Anthropic", "anthropic");
            Model model2 = createTestModel(2L, "claude-3-opus", provider2, "Claude 3 Opus", ModelStatus.ACTIVE);

            when(modelGateway.findAll()).thenReturn(List.of(testModel, model2));

            ModelQueryRequest request = new ModelQueryRequest();
            request.setProviderId(1L);
            request.setPage(1);
            request.setLimit(20);

            // when
            PageResponse<ModelResponse> response = modelService.query(request);

            // then
            assertThat(response.getItems()).hasSize(1);
            assertThat(response.getItems().get(0).getProviderId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("按状态过滤 - ACTIVE")
        void query_withStatusActive_filtersModels() {
            // given
            Provider provider2 = createTestProvider(2L, "ANTHROPIC", "Anthropic", "anthropic");
            Model model2 = createTestModel(2L, "claude-3-opus", provider2, "Claude 3 Opus", ModelStatus.DEPRECATED);

            when(modelGateway.findAll()).thenReturn(List.of(testModel, model2));

            ModelQueryRequest request = new ModelQueryRequest();
            request.setStatus(ModelStatus.ACTIVE);
            request.setPage(1);
            request.setLimit(20);

            // when
            PageResponse<ModelResponse> response = modelService.query(request);

            // then
            assertThat(response.getItems()).hasSize(1);
            assertThat(response.getItems().get(0).getStatus()).isEqualTo(ModelStatus.ACTIVE);
        }

        @Test
        @DisplayName("按状态过滤 - DEPRECATED")
        void query_withStatusDeprecated_filtersModels() {
            // given
            Provider provider2 = createTestProvider(2L, "ANTHROPIC", "Anthropic", "anthropic");
            Model model2 = createTestModel(2L, "claude-3-opus", provider2, "Claude 3 Opus", ModelStatus.DEPRECATED);

            when(modelGateway.findAll()).thenReturn(List.of(testModel, model2));

            ModelQueryRequest request = new ModelQueryRequest();
            request.setStatus(ModelStatus.DEPRECATED);
            request.setPage(1);
            request.setLimit(20);

            // when
            PageResponse<ModelResponse> response = modelService.query(request);

            // then
            assertThat(response.getItems()).hasSize(1);
            assertThat(response.getItems().get(0).getStatus()).isEqualTo(ModelStatus.DEPRECATED);
        }

        @Test
        @DisplayName("分页查询 - 第二页")
        void query_withPagination_returnsPagedModels() {
            // given
            List<Model> models = new ArrayList<>();
            for (long i = 1; i <= 25; i++) {
                Provider provider = createTestProvider(i, "PROVIDER_" + i, "Provider " + i, "provider-" + i);
                models.add(createTestModel(i, "model-" + i, provider, "Model " + i, ModelStatus.ACTIVE));
            }
            when(modelGateway.findAll()).thenReturn(models);

            ModelQueryRequest request = new ModelQueryRequest();
            request.setPage(2);
            request.setLimit(10);

            // when
            PageResponse<ModelResponse> response = modelService.query(request);

            // then
            assertThat(response.getItems()).hasSize(10);
            assertThat(response.getPagination().getPage()).isEqualTo(2);
            assertThat(response.getPagination().getLimit()).isEqualTo(10);
            assertThat(response.getPagination().getTotal()).isEqualTo(25);
            assertThat(response.getPagination().getTotalPages()).isEqualTo(3);
        }

        @Test
        @DisplayName("分页查询 - 最后一页不足 limit")
        void query_withPagination_lastPage_returnsRemainingModels() {
            // given
            List<Model> models = new ArrayList<>();
            for (long i = 1; i <= 25; i++) {
                Provider provider = createTestProvider(i, "PROVIDER_" + i, "Provider " + i, "provider-" + i);
                models.add(createTestModel(i, "model-" + i, provider, "Model " + i, ModelStatus.ACTIVE));
            }
            when(modelGateway.findAll()).thenReturn(models);

            ModelQueryRequest request = new ModelQueryRequest();
            request.setPage(3);
            request.setLimit(10);

            // when
            PageResponse<ModelResponse> response = modelService.query(request);

            // then
            assertThat(response.getItems()).hasSize(5);
            assertThat(response.getPagination().getPage()).isEqualTo(3);
            assertThat(response.getPagination().getTotal()).isEqualTo(25);
        }

        @Test
        @DisplayName("组合过滤 - 提供商 ID + 状态")
        void query_withProviderIdAndStatus_combinesFilters() {
            // given
            Provider provider2 = createTestProvider(2L, "ANTHROPIC", "Anthropic", "anthropic");
            Model model2 = createTestModel(2L, "claude-3-opus", provider2, "Claude 3 Opus", ModelStatus.DEPRECATED);

            when(modelGateway.findAll()).thenReturn(List.of(testModel, model2));

            ModelQueryRequest request = new ModelQueryRequest();
            request.setProviderId(2L);
            request.setStatus(ModelStatus.DEPRECATED);
            request.setPage(1);
            request.setLimit(20);

            // when
            PageResponse<ModelResponse> response = modelService.query(request);

            // then
            assertThat(response.getItems()).hasSize(1);
            assertThat(response.getItems().get(0).getProviderId()).isEqualTo(2L);
            assertThat(response.getItems().get(0).getStatus()).isEqualTo(ModelStatus.DEPRECATED);
        }
    }

    // ==================== update 测试 ====================

    @Nested
    @DisplayName("update 更新模型")
    class UpdateTests {

        @Test
        @DisplayName("更新 displayName 成功")
        void update_validDisplayName_updatesModel() {
            // given
            when(modelGateway.findById(1L)).thenReturn(Optional.of(testModel));
            when(modelGateway.save(any(Model.class))).thenReturn(testModel);

            ModelUpdateRequest request = new ModelUpdateRequest();
            request.setDisplayName("GPT-4 Updated");

            // when
            ModelResponse response = modelService.update(1L, request);

            // then
            assertThat(response).isNotNull();
            verify(modelGateway).findById(1L);
            verify(modelGateway).save(testModel);
        }

        @Test
        @DisplayName("更新 contextWindow 成功")
        void update_validContextWindow_updatesModel() {
            // given
            when(modelGateway.findById(1L)).thenReturn(Optional.of(testModel));
            when(modelGateway.save(any(Model.class))).thenReturn(testModel);

            ModelUpdateRequest request = new ModelUpdateRequest();
            request.setContextWindow(200000);

            // when
            modelService.update(1L, request);

            // then
            ArgumentCaptor<Model> modelCaptor = ArgumentCaptor.forClass(Model.class);
            verify(modelGateway).save(modelCaptor.capture());
            assertThat(modelCaptor.getValue().getContextWindow()).isEqualTo(200000);
        }

        @Test
        @DisplayName("更新 inputPrice 成功")
        void update_validInputPrice_updatesModel() {
            // given
            when(modelGateway.findById(1L)).thenReturn(Optional.of(testModel));
            when(modelGateway.save(any(Model.class))).thenReturn(testModel);

            ModelUpdateRequest request = new ModelUpdateRequest();
            request.setInputPrice(new BigDecimal("0.000006"));

            // when
            modelService.update(1L, request);

            // then
            ArgumentCaptor<Model> modelCaptor = ArgumentCaptor.forClass(Model.class);
            verify(modelGateway).save(modelCaptor.capture());
            assertThat(modelCaptor.getValue().getInputPrice()).isEqualTo(new BigDecimal("0.000006"));
        }

        @Test
        @DisplayName("更新 outputPrice 成功")
        void update_validOutputPrice_updatesModel() {
            // given
            when(modelGateway.findById(1L)).thenReturn(Optional.of(testModel));
            when(modelGateway.save(any(Model.class))).thenReturn(testModel);

            ModelUpdateRequest request = new ModelUpdateRequest();
            request.setOutputPrice(new BigDecimal("0.000018"));

            // when
            modelService.update(1L, request);

            // then
            ArgumentCaptor<Model> modelCaptor = ArgumentCaptor.forClass(Model.class);
            verify(modelGateway).save(modelCaptor.capture());
            assertThat(modelCaptor.getValue().getOutputPrice()).isEqualTo(new BigDecimal("0.000018"));
        }

        @Test
        @DisplayName("更新 capabilities 成功")
        void update_validCapabilities_updatesModel() {
            // given
            when(modelGateway.findById(1L)).thenReturn(Optional.of(testModel));
            when(modelGateway.save(any(Model.class))).thenReturn(testModel);

            Map<String, Boolean> capabilities = new HashMap<>();
            capabilities.put("vision", true);
            capabilities.put("function_calling", true);

            ModelUpdateRequest request = new ModelUpdateRequest();
            request.setCapabilities(capabilities);

            // when
            modelService.update(1L, request);

            // then
            ArgumentCaptor<Model> modelCaptor = ArgumentCaptor.forClass(Model.class);
            verify(modelGateway).save(modelCaptor.capture());
            assertThat(modelCaptor.getValue().getCapabilities()).containsEntry("vision", true);
            assertThat(modelCaptor.getValue().getCapabilities()).containsEntry("function_calling", true);
        }

        @Test
        @DisplayName("更新 enabled=true 设置状态为 ACTIVE")
        void update_enabledTrue_setsStatusActive() {
            // given
            testModel.setStatus(ModelStatus.DEPRECATED);
            when(modelGateway.findById(1L)).thenReturn(Optional.of(testModel));
            when(modelGateway.save(any(Model.class))).thenReturn(testModel);

            ModelUpdateRequest request = new ModelUpdateRequest();
            request.setEnabled(true);

            // when
            modelService.update(1L, request);

            // then
            ArgumentCaptor<Model> modelCaptor = ArgumentCaptor.forClass(Model.class);
            verify(modelGateway).save(modelCaptor.capture());
            assertThat(modelCaptor.getValue().getStatus()).isEqualTo(ModelStatus.ACTIVE);
        }

        @Test
        @DisplayName("更新 enabled=false 设置状态为 DEPRECATED")
        void update_enabledFalse_setsStatusDeprecated() {
            // given
            testModel.setStatus(ModelStatus.ACTIVE);
            when(modelGateway.findById(1L)).thenReturn(Optional.of(testModel));
            when(modelGateway.save(any(Model.class))).thenReturn(testModel);

            ModelUpdateRequest request = new ModelUpdateRequest();
            request.setEnabled(false);

            // when
            modelService.update(1L, request);

            // then
            ArgumentCaptor<Model> modelCaptor = ArgumentCaptor.forClass(Model.class);
            verify(modelGateway).save(modelCaptor.capture());
            assertThat(modelCaptor.getValue().getStatus()).isEqualTo(ModelStatus.DEPRECATED);
        }

        @Test
        @DisplayName("更新不存在的模型抛出异常")
        void update_nonExistingModel_throwsException() {
            // given
            when(modelGateway.findById(99L)).thenReturn(Optional.empty());

            ModelUpdateRequest request = new ModelUpdateRequest();
            request.setDisplayName("Updated Name");

            // when & then
            assertThatThrownBy(() -> modelService.update(99L, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Model")
                .hasMessageContaining("99");
        }

        @Test
        @DisplayName("更新多个字段成功")
        void update_multipleFields_updatesAllFields() {
            // given
            when(modelGateway.findById(1L)).thenReturn(Optional.of(testModel));
            when(modelGateway.save(any(Model.class))).thenReturn(testModel);

            ModelUpdateRequest request = new ModelUpdateRequest();
            request.setDisplayName("GPT-4 Turbo");
            request.setContextWindow(128000);
            request.setInputPrice(new BigDecimal("0.000010"));
            request.setOutputPrice(new BigDecimal("0.000030"));
            request.setEnabled(true);

            // when
            modelService.update(1L, request);

            // then
            verify(modelGateway).save(testModel);
        }
    }

    // ==================== delete 测试 ====================

    @Nested
    @DisplayName("delete 删除模型")
    class DeleteTests {

        @Test
        @DisplayName("删除模型成功（软删除）")
        void delete_existingModel_softDeletes() {
            // given
            when(modelGateway.findById(1L)).thenReturn(Optional.of(testModel));
            when(modelGateway.save(any(Model.class))).thenReturn(testModel);

            // when
            modelService.delete(1L);

            // then
            ArgumentCaptor<Model> modelCaptor = ArgumentCaptor.forClass(Model.class);
            verify(modelGateway).save(modelCaptor.capture());
            assertThat(testModel.getDeletedAt()).isNotNull();
        }

        @Test
        @DisplayName("删除不存在的模型抛出异常")
        void delete_nonExistingModel_throwsException() {
            // given
            when(modelGateway.findById(99L)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> modelService.delete(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Model")
                .hasMessageContaining("99");

            verify(modelGateway, never()).save(any(Model.class));
        }
    }

    // ==================== setEnabled 测试 ====================

    @Nested
    @DisplayName("setEnabled 启用/禁用模型")
    class SetEnabledTests {

        @Test
        @DisplayName("启用模型成功")
        void setEnabled_true_activatesModel() {
            // given
            testModel.setStatus(ModelStatus.DEPRECATED);
            when(modelGateway.findById(1L)).thenReturn(Optional.of(testModel));
            when(modelGateway.save(any(Model.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // when
            ModelResponse response = modelService.setEnabled(1L, true);

            // then
            assertThat(testModel.getStatus()).isEqualTo(ModelStatus.ACTIVE);
            assertThat(response.getStatus()).isEqualTo(ModelStatus.ACTIVE);
            assertThat(response.getEnabled()).isTrue();
            verify(modelGateway).save(testModel);
        }

        @Test
        @DisplayName("禁用模型成功")
        void setEnabled_false_deprecatesModel() {
            // given
            testModel.setStatus(ModelStatus.ACTIVE);
            when(modelGateway.findById(1L)).thenReturn(Optional.of(testModel));
            when(modelGateway.save(any(Model.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // when
            ModelResponse response = modelService.setEnabled(1L, false);

            // then
            assertThat(testModel.getStatus()).isEqualTo(ModelStatus.DEPRECATED);
            assertThat(response.getStatus()).isEqualTo(ModelStatus.DEPRECATED);
            assertThat(response.getEnabled()).isFalse();
            verify(modelGateway).save(testModel);
        }

        @Test
        @DisplayName("启用不存在的模型抛出异常")
        void setEnabled_nonExistingModel_throwsException() {
            // given
            when(modelGateway.findById(99L)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> modelService.setEnabled(99L, true))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Model")
                .hasMessageContaining("99");
        }
    }

    // ==================== 辅助方法 ====================

    private Provider createTestProvider(Long id, String providerCode, String providerName, String code) {
        Provider provider = new Provider();
        provider.setId(id);
        provider.setProviderCode(providerCode);
        provider.setProviderName(providerName);
        provider.setProviderCode(code);
        return provider;
    }

    private Model createTestModel(Long id, String modelCode, Provider provider, String displayName, ModelStatus status) {
        Model model = new Model();
        model.setId(id);
        model.setModelCode(modelCode);
        model.setProvider(provider);
        model.setProviderModelId(modelCode + "-0613");
        model.setDisplayName(displayName);
        model.setContextWindow(8000);
        model.setInputPrice(new BigDecimal("0.00003"));
        model.setOutputPrice(new BigDecimal("0.00006"));
        model.setCapabilities(Map.of("vision", false, "function_calling", true));
        model.setStatus(status);
        model.setCreatedAt(Instant.now());
        model.setUpdatedAt(Instant.now());
        return model;
    }
}