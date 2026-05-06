package com.codingas.gateway.domain.model.service;

import com.codingas.gateway.common.enums.ProviderType;
import com.codingas.gateway.domain.model.entity.Model;
import com.codingas.gateway.domain.model.entity.Provider;
import com.codingas.gateway.domain.model.gateway.ModelGateway;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * ModelDomainService 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ModelDomainService 测试")
class ModelDomainServiceTest {

    @Mock
    private ModelGateway modelGateway;

    @InjectMocks
    private ModelDomainService service;

    @Nested
    @DisplayName("findAll 方法测试")
    class FindAllTests {

        @Test
        @DisplayName("返回所有活跃 Model")
        void findAll_returnsActiveModels() {
            // given
            Model model = createTestModel();
            when(modelGateway.findAllActive()).thenReturn(List.of(model));

            // when
            List<Model> result = service.findAll();

            // then
            assertThat(result).hasSize(1);
        }
    }

    @Nested
    @DisplayName("findById 方法测试")
    class FindByIdTests {

        @Test
        @DisplayName("找到 Model 返回实体")
        void findById_existingId_returnsEntity() {
            // given
            Model model = createTestModel();
            when(modelGateway.findById(1L)).thenReturn(Optional.of(model));

            // when
            Optional<Model> result = service.findById(1L);

            // then
            assertThat(result).isPresent();
            assertThat(result.get().getId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("未找到返回空")
        void findById_nonExistingId_returnsEmpty() {
            // given
            when(modelGateway.findById(999L)).thenReturn(Optional.empty());

            // when
            Optional<Model> result = service.findById(999L);

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByProviderId 方法测试")
    class FindByProviderIdTests {

        @Test
        @DisplayName("通过提供商 ID 找到 Model 列表")
        void findByProviderId_existingProviderId_returnsList() {
            // given
            Model model = createTestModel();
            when(modelGateway.findByProviderId(1L)).thenReturn(List.of(model));

            // when
            List<Model> result = service.findByProviderId(1L);

            // then
            assertThat(result).hasSize(1);
        }
    }

    @Nested
    @DisplayName("create 方法测试")
    class CreateTests {

        @Test
        @DisplayName("创建 Model 成功")
        void create_validModel_returnsCreated() {
            // given
            Model model = createTestModel();
            when(modelGateway.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // when
            Model result = service.create(model);

            // then
            assertThat(result.getId()).isEqualTo(1L);
        }
    }

    @Nested
    @DisplayName("update 方法测试")
    class UpdateTests {

        @Test
        @DisplayName("更新 Model 成功")
        void update_existingModel_returnsUpdated() {
            // given
            Model existing = createTestModel();
            Model updateData = new Model();
            updateData.setDisplayName("Updated Display Name");
            updateData.setContextWindow(16384);
            updateData.setInputPrice(BigDecimal.valueOf(0.05));
            updateData.setOutputPrice(BigDecimal.valueOf(0.10));
            updateData.setCapabilities(Map.of("chat", true, "vision", true));
            updateData.setStatus(Model.ModelStatus.DEPRECATED);

            when(modelGateway.findById(1L)).thenReturn(Optional.of(existing));
            when(modelGateway.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // when
            Model result = service.update(1L, updateData);

            // then
            assertThat(result.getDisplayName()).isEqualTo("Updated Display Name");
            assertThat(result.getContextWindow()).isEqualTo(16384);
            assertThat(result.getStatus()).isEqualTo(Model.ModelStatus.DEPRECATED);
        }

        @Test
        @DisplayName("更新不存在的 Model 抛出异常")
        void update_nonExistingModel_throwsException() {
            // given
            when(modelGateway.findById(999L)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> service.update(999L, new Model()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Model not found");
        }
    }

    @Nested
    @DisplayName("delete 方法测试")
    class DeleteTests {

        @Test
        @DisplayName("删除 Model 成功")
        void delete_existingModel_marksAsDeleted() {
            // given
            Model model = createTestModel();
            when(modelGateway.findById(1L)).thenReturn(Optional.of(model));
            when(modelGateway.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // when
            service.delete(1L);

            // then
            assertThat(model.getStatus()).isEqualTo(Model.ModelStatus.DELETED);
        }

        @Test
        @DisplayName("删除不存在的 Model 抛出异常")
        void delete_nonExistingModel_throwsException() {
            // given
            when(modelGateway.findById(999L)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> service.delete(999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Model not found");
        }
    }

    // Helper methods
    private Model createTestModel() {
        Model model = new Model();
        model.setId(1L);
        model.setProviderModelId("gpt-4");
        model.setDisplayName("GPT-4");
        model.setContextWindow(8192);
        model.setInputPrice(BigDecimal.valueOf(0.03));
        model.setOutputPrice(BigDecimal.valueOf(0.06));
        model.setCapabilities(Map.of("chat", true, "streaming", true));
        model.setStatus(Model.ModelStatus.ACTIVE);

        Provider provider = new Provider();
        provider.setId(1L);
        provider.setProviderName("OpenAI");
        provider.setProviderType(ProviderType.OPENAI);
        model.setProvider(provider);

        return model;
    }
}
