package com.codingas.gateway.application.model;

import com.codingas.gateway.application.model.dto.ModelCreateRequest;
import com.codingas.gateway.application.model.dto.ModelQueryRequest;
import com.codingas.gateway.application.model.dto.ModelResponse;
import com.codingas.gateway.application.model.dto.ModelUpdateRequest;
import com.codingas.gateway.common.dto.PageResponse;
import com.codingas.gateway.common.exception.ResourceNotFoundException;
import com.codingas.gateway.domain.supply.entity.Model;
import com.codingas.gateway.domain.supply.entity.Provider;
import com.codingas.gateway.domain.supply.gateway.ModelGateway;
import com.codingas.gateway.domain.supply.gateway.ProviderGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.ArrayList;
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
        testProvider = createTestProvider(1L, "OpenAI");
        testModel = createTestModel(1L, "gpt-4", testProvider, "GPT-4", true);
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
            request.setModelName("gpt-4o-2024-08-06");
            request.setDisplayName("GPT-4o");
            request.setContextWindow(128000);

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
            assertThat(response.getModelName()).isEqualTo("gpt-4o-2024-08-06");
            assertThat(response.getDisplayName()).isEqualTo("GPT-4o");
            assertThat(response.getContextWindow()).isEqualTo(128000);

            verify(modelGateway).save(any(Model.class));
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
            assertThat(response.getDisplayName()).isEqualTo("GPT-4");
            // 默认（未废弃）模型状态应为 ACTIVE
            assertThat(response.getState()).isEqualTo("ACTIVE");

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
            Provider provider2 = createTestProvider(2L, "Anthropic");
            Model model2 = createTestModel(2L, "claude-3-opus", provider2, "Claude 3 Opus", true);

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
        @DisplayName("关键字过滤 - 匹配 providerModelId")
        void query_withKeyword_matchesProviderModelId() {
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
            assertThat(response.getItems().get(0).getModelName()).isEqualTo("gpt-4");
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
        @DisplayName("按提供商 ID 过滤（暂不按 providerId 过滤，返回全部）")
        void query_withProviderId_returnsAllModels() {
            // given：providerId 过滤已移除，返回所有模型
            Provider provider2 = createTestProvider(2L, "Anthropic");
            Model model2 = createTestModel(2L, "claude-3-opus", provider2, "Claude 3 Opus", true);

            when(modelGateway.findAll()).thenReturn(List.of(testModel, model2));

            ModelQueryRequest request = new ModelQueryRequest();
            request.setProviderId(1L);
            request.setPage(1);
            request.setLimit(20);

            // when
            PageResponse<ModelResponse> response = modelService.query(request);

            // then：providerId 过滤暂不生效，返回所有模型
            assertThat(response.getItems()).hasSize(2);
        }

        @Test
        @DisplayName("状态过滤 - ACTIVE 仅返回未废弃模型")
        void query_withStateActive_returnsOnlyActiveModels() {
            // given：一个启用、一个禁用
            Provider provider2 = createTestProvider(2L, "Anthropic");
            Model disabledModel = createTestModel(2L, "claude-3-opus", provider2, "Claude 3 Opus", false);

            when(modelGateway.findAll()).thenReturn(List.of(testModel, disabledModel));

            ModelQueryRequest request = new ModelQueryRequest();
            request.setState("ACTIVE");
            request.setPage(1);
            request.setLimit(20);

            // when
            PageResponse<ModelResponse> response = modelService.query(request);

            // then：仅返回启用的模型
            assertThat(response.getItems()).hasSize(1);
            assertThat(response.getItems().get(0).getState()).isEqualTo("ACTIVE");
            assertThat(response.getPagination().getTotal()).isEqualTo(1);
        }

        @Test
        @DisplayName("状态过滤 - INACTIVE 仅返回已废弃模型")
        void query_withStateInactive_returnsOnlyDeprecatedModels() {
            // given：一个启用、一个禁用
            Provider provider2 = createTestProvider(2L, "Anthropic");
            Model disabledModel = createTestModel(2L, "claude-3-opus", provider2, "Claude 3 Opus", false);

            when(modelGateway.findAll()).thenReturn(List.of(testModel, disabledModel));

            ModelQueryRequest request = new ModelQueryRequest();
            request.setState("INACTIVE");
            request.setPage(1);
            request.setLimit(20);

            // when
            PageResponse<ModelResponse> response = modelService.query(request);

            // then：仅返回禁用的模型
            assertThat(response.getItems()).hasSize(1);
            assertThat(response.getItems().get(0).getState()).isEqualTo("INACTIVE");
        }

        @Test
        @DisplayName("分页查询 - 第二页")
        void query_withPagination_returnsPagedModels() {
            // given
            List<Model> models = new ArrayList<>();
            for (long i = 1; i <= 25; i++) {
                Provider provider = createTestProvider(i, "Provider " + i);
                models.add(createTestModel(i, "model-" + i, provider, "Model " + i, true));
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
            doNothing().when(modelGateway).delete(any(Model.class));

            // when
            modelService.delete(1L);

            // then
            verify(modelGateway).delete(any(Model.class));
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
            // given：先构造一个已废弃的模型
            Model deprecatedModel = createTestModel(1L, "gpt-4", testProvider, "GPT-4", false);
            when(modelGateway.findById(1L)).thenReturn(Optional.of(deprecatedModel));
            when(modelGateway.save(any(Model.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // when
            ModelResponse response = modelService.setEnabled(1L, true);

            // then：deprecatedAt 被清空，状态为 ACTIVE
            assertThat(response).isNotNull();
            assertThat(deprecatedModel.getDeprecatedAt()).isNull();
            assertThat(response.getState()).isEqualTo("ACTIVE");
            verify(modelGateway).save(deprecatedModel);
        }

        @Test
        @DisplayName("禁用模型成功")
        void setEnabled_false_deprecatesModel() {
            // given
            when(modelGateway.findById(1L)).thenReturn(Optional.of(testModel));
            when(modelGateway.save(any(Model.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // when
            ModelResponse response = modelService.setEnabled(1L, false);

            // then：deprecatedAt 被设置，状态为 INACTIVE
            assertThat(response).isNotNull();
            assertThat(testModel.getDeprecatedAt()).isNotNull();
            assertThat(response.getState()).isEqualTo("INACTIVE");
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

    private Provider createTestProvider(Long id, String providerName) {
        Provider provider = new Provider();
        provider.setId(id);
        provider.setName(providerName);
        return provider;
    }

    private Model createTestModel(Long id, String providerModelId, Provider provider, String displayName, Boolean available) {
        Model model = new Model();
        model.setId(id);
        model.setModelName(providerModelId);
        model.setDisplayName(displayName);
        model.setContextWindow(8000);
        model.setCapabilities(Map.of("vision", false, "function_calling", true));
        // available=false 表示已废弃（deprecatedAt 非空），对应状态 INACTIVE
        if (available != null && !available) {
            model.setDeprecatedAt(Instant.now());
        }
        model.setCreatedAt(Instant.now());
        model.setUpdatedAt(Instant.now());
        return model;
    }
}
