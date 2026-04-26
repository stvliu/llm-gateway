package com.codingas.gateway.core.service;

import com.codingas.gateway.core.domain.entity.Model;
import com.codingas.gateway.core.domain.gateway.ModelGateway;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * ModelService 单元测试
 *
 * <p>测试 ModelService 通过 ModelGateway 接口访问持久化数据。</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ModelService Tests")
class ModelServiceTest {

    @Mock
    private ModelGateway modelGateway;

    @InjectMocks
    private ModelService modelService;

    private Model testModel;

    @BeforeEach
    void setUp() {
        testModel = new Model();
        testModel.setId(1L);
        testModel.setModelCode("gpt-4o");
        testModel.setProviderId(100L);
        testModel.setProviderModelId("gpt-4o");
        testModel.setDisplayName("GPT-4o");
        testModel.setContextWindow(128000);
        testModel.setInputPrice(new BigDecimal("2.50"));
        testModel.setOutputPrice(new BigDecimal("10.00"));
        testModel.setCapabilities(Map.of("streaming", true, "vision", true));
        testModel.setStatus(Model.ModelStatus.ACTIVE);
    }

    @Nested
    @DisplayName("findAll")
    class FindAllTests {

        @Test
        @DisplayName("返回所有活跃模型")
        void findAll_returnsActiveModels() {
            List<Model> models = List.of(testModel);
            when(modelGateway.findAllActive()).thenReturn(models);

            List<Model> result = modelService.findAll();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getModelCode()).isEqualTo("gpt-4o");
            verify(modelGateway).findAllActive();
        }

        @Test
        @DisplayName("返回空列表当没有活跃模型时")
        void findAll_emptyList() {
            when(modelGateway.findAllActive()).thenReturn(List.of());

            List<Model> result = modelService.findAll();

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findById")
    class FindByIdTests {

        @Test
        @DisplayName("返回模型当存在时")
        void findById_existingModel() {
            when(modelGateway.findById(1L)).thenReturn(Optional.of(testModel));

            Optional<Model> result = modelService.findById(1L);

            assertThat(result).isPresent();
            assertThat(result.get().getModelCode()).isEqualTo("gpt-4o");
        }

        @Test
        @DisplayName("返回空 Optional 当不存在时")
        void findById_notFound() {
            when(modelGateway.findById(99L)).thenReturn(Optional.empty());

            Optional<Model> result = modelService.findById(99L);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByModelCode")
    class FindByModelCodeTests {

        @Test
        @DisplayName("根据模型编码查询成功")
        void findByModelCode_success() {
            when(modelGateway.findByModelCode("gpt-4o")).thenReturn(Optional.of(testModel));

            Optional<Model> result = modelService.findByModelCode("gpt-4o");

            assertThat(result).isPresent();
            assertThat(result.get().getDisplayName()).isEqualTo("GPT-4o");
        }

        @Test
        @DisplayName("根据模型编码查询失败")
        void findByModelCode_notFound() {
            when(modelGateway.findByModelCode("unknown")).thenReturn(Optional.empty());

            Optional<Model> result = modelService.findByModelCode("unknown");

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByProviderId")
    class FindByProviderIdTests {

        @Test
        @DisplayName("返回指定 Provider 的所有模型")
        void findByProviderId_returnsModels() {
            List<Model> models = List.of(testModel);
            when(modelGateway.findByProviderCode("100")).thenReturn(models);

            List<Model> result = modelService.findByProviderId(100L);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getProviderId()).isEqualTo(100L);
        }
    }

    @Nested
    @DisplayName("create")
    class CreateTests {

        @Test
        @DisplayName("创建模型成功")
        void create_success() {
            when(modelGateway.save(any(Model.class))).thenReturn(testModel);

            Model result = modelService.create(testModel);

            assertThat(result).isNotNull();
            assertThat(result.getModelCode()).isEqualTo("gpt-4o");

            ArgumentCaptor<Model> captor = ArgumentCaptor.forClass(Model.class);
            verify(modelGateway).save(captor.capture());
            assertThat(captor.getValue().getModelCode()).isEqualTo("gpt-4o");
        }
    }

    @Nested
    @DisplayName("update")
    class UpdateTests {

        @Test
        @DisplayName("更新模型成功")
        void update_success() {
            Model updatedModel = new Model();
            updatedModel.setDisplayName("GPT-4o Updated");
            updatedModel.setContextWindow(256000);
            updatedModel.setInputPrice(new BigDecimal("3.00"));
            updatedModel.setOutputPrice(new BigDecimal("12.00"));
            updatedModel.setCapabilities(Map.of("streaming", true));
            updatedModel.setStatus(Model.ModelStatus.ACTIVE);

            when(modelGateway.findById(1L)).thenReturn(Optional.of(testModel));
            when(modelGateway.save(any(Model.class))).thenAnswer(inv -> inv.getArgument(0));

            Model result = modelService.update(1L, updatedModel);

            assertThat(result.getDisplayName()).isEqualTo("GPT-4o Updated");
            assertThat(result.getContextWindow()).isEqualTo(256000);
            assertThat(result.getInputPrice()).isEqualByComparingTo(new BigDecimal("3.00"));
        }

        @Test
        @DisplayName("更新不存在的模型抛出异常")
        void update_notFound() {
            when(modelGateway.findById(99L)).thenReturn(Optional.empty());

            Model updatedModel = new Model();
            updatedModel.setDisplayName("Updated");

            assertThatThrownBy(() -> modelService.update(99L, updatedModel))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Model not found: 99");
        }
    }

    @Nested
    @DisplayName("delete")
    class DeleteTests {

        @Test
        @DisplayName("删除存在的模型成功")
        void delete_success() {
            when(modelGateway.findById(1L)).thenReturn(Optional.of(testModel));
            when(modelGateway.save(any(Model.class))).thenReturn(testModel);

            modelService.delete(1L);

            verify(modelGateway).save(testModel);
        }

        @Test
        @DisplayName("删除不存在的模型抛出异常")
        void delete_notFound() {
            when(modelGateway.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> modelService.delete(99L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Model not found: 99");
        }
    }
}
