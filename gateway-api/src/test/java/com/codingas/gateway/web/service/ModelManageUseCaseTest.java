package com.codingas.gateway.web.service;

import com.codingas.gateway.core.domain.entity.Model;
import com.codingas.gateway.core.service.ModelService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * ModelManageUseCase 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ModelManageUseCase Tests")
class ModelManageUseCaseTest {

    @Mock
    private ModelService modelService;

    @InjectMocks
    private ModelManageUseCase modelManageUseCase;

    private Model testModel;

    @BeforeEach
    void setUp() {
        testModel = new Model();
        testModel.setId(1L);
        testModel.setModelCode("gpt-4o");
        testModel.setProviderId(1L);
        testModel.setProviderModelId("gpt-4o");
        testModel.setDisplayName("GPT-4o");
        testModel.setContextWindow(128000);
        testModel.setInputPrice(BigDecimal.valueOf(2.5));
        testModel.setOutputPrice(BigDecimal.valueOf(10.0));
        testModel.setCapabilities(Map.of("streaming", true, "function_calling", true));
        testModel.setStatus(Model.ModelStatus.ACTIVE);
    }

    @Nested
    @DisplayName("findAll")
    class FindAllTests {

        @Test
        @DisplayName("返回所有模型")
        void findAll_returnsAllModels() {
            when(modelService.findAll()).thenReturn(List.of(testModel));

            List<Model> result = modelManageUseCase.findAll();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getModelCode()).isEqualTo("gpt-4o");
            verify(modelService).findAll();
        }

        @Test
        @DisplayName("无模型时返回空列表")
        void findAll_empty_returnsEmptyList() {
            when(modelService.findAll()).thenReturn(List.of());

            List<Model> result = modelManageUseCase.findAll();

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findById")
    class FindByIdTests {

        @Test
        @DisplayName("存在则返回模型")
        void findById_exists_returnsModel() {
            when(modelService.findById(1L)).thenReturn(Optional.of(testModel));

            Optional<Model> result = modelManageUseCase.findById(1L);

            assertThat(result).isPresent();
            assertThat(result.get().getModelCode()).isEqualTo("gpt-4o");
        }

        @Test
        @DisplayName("不存在则返回空")
        void findById_notExists_returnsEmpty() {
            when(modelService.findById(99L)).thenReturn(Optional.empty());

            Optional<Model> result = modelManageUseCase.findById(99L);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByModelCode")
    class FindByModelCodeTests {

        @Test
        @DisplayName("根据编码查找模型")
        void findByModelCode_exists_returnsModel() {
            when(modelService.findByModelCode("gpt-4o")).thenReturn(Optional.of(testModel));

            Optional<Model> result = modelManageUseCase.findByModelCode("gpt-4o");

            assertThat(result).isPresent();
            assertThat(result.get().getDisplayName()).isEqualTo("GPT-4o");
        }
    }

    @Nested
    @DisplayName("findByProviderId")
    class FindByProviderIdTests {

        @Test
        @DisplayName("返回指定提供商的模型")
        void findByProviderId_returnsModels() {
            when(modelService.findByProviderId(1L)).thenReturn(List.of(testModel));

            List<Model> result = modelManageUseCase.findByProviderId(1L);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getProviderId()).isEqualTo(1L);
        }
    }

    @Nested
    @DisplayName("create")
    class CreateTests {

        @Test
        @DisplayName("创建模型并返回")
        void create_validModel_returnsCreated() {
            Model newModel = new Model();
            newModel.setModelCode("gpt-4o-mini");
            newModel.setDisplayName("GPT-4o Mini");
            newModel.setProviderId(1L);
            newModel.setProviderModelId("gpt-4o-mini");
            when(modelService.create(any(Model.class))).thenReturn(newModel);

            Model result = modelManageUseCase.create(newModel);

            assertThat(result.getModelCode()).isEqualTo("gpt-4o-mini");
            verify(modelService).create(any(Model.class));
        }
    }

    @Nested
    @DisplayName("update")
    class UpdateTests {

        @Test
        @DisplayName("更新模型并返回")
        void update_validModel_returnsUpdated() {
            Model updated = new Model();
            updated.setModelCode("gpt-4o");
            updated.setDisplayName("GPT-4o Updated");
            when(modelService.update(eq(1L), any(Model.class))).thenReturn(updated);

            Model result = modelManageUseCase.update(1L, updated);

            assertThat(result.getDisplayName()).isEqualTo("GPT-4o Updated");
            verify(modelService).update(eq(1L), any(Model.class));
        }
    }

    @Nested
    @DisplayName("delete")
    class DeleteTests {

        @Test
        @DisplayName("删除模型")
        void delete_validId_callsService() {
            doNothing().when(modelService).delete(1L);

            modelManageUseCase.delete(1L);

            verify(modelService).delete(1L);
        }
    }
}