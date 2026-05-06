package com.codingas.gateway.application.model;

import com.codingas.gateway.domain.model.entity.Model;
import com.codingas.gateway.domain.model.entity.Provider;
import com.codingas.gateway.domain.model.service.ModelDomainService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * ModelManageUseCase 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ModelManageUseCase 单元测试")
class ModelManageUseCaseTest {

    @Mock
    private ModelDomainService modelService;

    @InjectMocks
    private ModelManageUseCase modelManageUseCase;

    @Nested
    @DisplayName("findAll")
    class FindAllTests {

        @Test
        @DisplayName("返回所有模型")
        void findAll_returnsAllModels() {
            // given
            Model model1 = createModel(1L, "gpt-4", "GPT-4");
            Model model2 = createModel(2L, "gpt-3.5", "GPT-3.5");
            when(modelService.findAll()).thenReturn(List.of(model1, model2));

            // when
            List<Model> result = modelManageUseCase.findAll();

            // then
            assertThat(result).hasSize(2);
            assertThat(result.get(0).getProviderModelId()).isEqualTo("gpt-4");
            assertThat(result.get(1).getProviderModelId()).isEqualTo("gpt-3.5");
            verify(modelService).findAll();
        }
    }

    @Nested
    @DisplayName("findById")
    class FindByIdTests {

        @Test
        @DisplayName("模型存在时返回模型")
        void findById_existingModel_returnsModel() {
            // given
            Long id = 1L;
            Model model = createModel(id, "gpt-4", "GPT-4");
            when(modelService.findById(id)).thenReturn(Optional.of(model));

            // when
            Optional<Model> result = modelManageUseCase.findById(id);

            // then
            assertThat(result).isPresent();
            assertThat(result.get().getProviderModelId()).isEqualTo("gpt-4");
            verify(modelService).findById(id);
        }

        @Test
        @DisplayName("模型不存在时返回空 Optional")
        void findById_nonExistingModel_returnsEmpty() {
            // given
            Long id = 99L;
            when(modelService.findById(id)).thenReturn(Optional.empty());

            // when
            Optional<Model> result = modelManageUseCase.findById(id);

            // then
            assertThat(result).isEmpty();
            verify(modelService).findById(id);
        }
    }

    @Nested
    @DisplayName("create")
    class CreateTests {

        @Test
        @DisplayName("创建模型成功")
        void create_validModel_returnsCreatedModel() {
            // given
            Model inputModel = createModel(null, "gpt-4", "GPT-4");
            Model savedModel = createModel(1L, "gpt-4", "GPT-4");
            when(modelService.create(any(Model.class))).thenReturn(savedModel);

            // when
            Model result = modelManageUseCase.create(inputModel);

            // then
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getProviderModelId()).isEqualTo("gpt-4");
            verify(modelService).create(any(Model.class));
        }
    }

    @Nested
    @DisplayName("update")
    class UpdateTests {

        @Test
        @DisplayName("更新模型成功")
        void update_validModel_returnsUpdatedModel() {
            // given
            Long id = 1L;
            Model updateModel = createModel(null, "gpt-4-updated", "GPT-4 Updated");
            Model updatedModel = createModel(id, "gpt-4-updated", "GPT-4 Updated");
            when(modelService.update(eq(id), any(Model.class))).thenReturn(updatedModel);

            // when
            Model result = modelManageUseCase.update(id, updateModel);

            // then
            assertThat(result.getId()).isEqualTo(id);
            assertThat(result.getDisplayName()).isEqualTo("GPT-4 Updated");
            verify(modelService).update(eq(id), any(Model.class));
        }
    }

    @Nested
    @DisplayName("delete")
    class DeleteTests {

        @Test
        @DisplayName("删除模型成功")
        void delete_existingModel_deletesSuccessfully() {
            // given
            Long id = 1L;
            doNothing().when(modelService).delete(id);

            // when
            modelManageUseCase.delete(id);

            // then
            verify(modelService).delete(id);
        }
    }

    /**
     * 创建测试用 Model 对象
     */
    private Model createModel(Long id, String providerModelId, String displayName) {
        Model model = new Model();
        model.setId(id);
        model.setProviderModelId(providerModelId);
        model.setDisplayName(displayName);
        Provider provider = new Provider();
        provider.setId(1L);
        model.setProvider(provider);
        model.setContextWindow(8000);
        model.setInputPrice(new BigDecimal("0.03"));
        model.setOutputPrice(new BigDecimal("0.06"));
        model.setStatus(Model.ModelStatus.ACTIVE);
        return model;
    }
}
