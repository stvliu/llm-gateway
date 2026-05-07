package com.codingas.gateway.infrastructure.model.gateway;

import com.codingas.gateway.common.enums.ProviderType;
import com.codingas.gateway.domain.model.entity.Model;
import com.codingas.gateway.infrastructure.model.gateway.database.ModelRepository;
import com.codingas.gateway.infrastructure.model.gateway.database.dataobject.ModelDo;
import com.codingas.gateway.infrastructure.model.gateway.database.dataobject.ProviderDo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * ModelGatewayImpl 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ModelGatewayImpl 测试")
class ModelGatewayImplTest {

    @Mock
    private ModelRepository modelRepository;

    @InjectMocks
    private ModelGatewayImpl gateway;

    @Nested
    @DisplayName("save 方法测试")
    class SaveTests {

        @Test
        @DisplayName("保存 Model 成功")
        void save_validEntity_returnsSaved() {
            // given
            Model entity = createTestEntity();
            ModelDo savedDo = createTestDo();

            when(modelRepository.save(any())).thenReturn(savedDo);

            // when
            Model result = gateway.save(entity);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            verify(modelRepository).save(any());
        }
    }

    @Nested
    @DisplayName("findById 方法测试")
    class FindByIdTests {

        @Test
        @DisplayName("找到 Model 返回实体")
        void findById_existingId_returnsEntity() {
            // given
            ModelDo doEntity = createTestDo();
            when(modelRepository.findById(1L)).thenReturn(Optional.of(doEntity));

            // when
            Optional<Model> result = gateway.findById(1L);

            // then
            assertThat(result).isPresent();
            assertThat(result.get().getId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("未找到返回空")
        void findById_nonExistingId_returnsEmpty() {
            // given
            when(modelRepository.findById(999L)).thenReturn(Optional.empty());

            // when
            Optional<Model> result = gateway.findById(999L);

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findAll 方法测试")
    class FindAllTests {

        @Test
        @DisplayName("返回所有 Model")
        void findAll_returnsAll() {
            // given
            ModelDo doEntity1 = createTestDo();
            ModelDo doEntity2 = createTestDo();
            doEntity2.setId(2L);
            when(modelRepository.findAll()).thenReturn(List.of(doEntity1, doEntity2));

            // when
            List<Model> result = gateway.findAll();

            // then
            assertThat(result).hasSize(2);
        }
    }

    @Nested
    @DisplayName("findAllActive 方法测试")
    class FindAllActiveTests {

        @Test
        @DisplayName("返回所有活跃 Model")
        void findAllActive_returnsActiveModels() {
            // given
            ModelDo doEntity = createTestDo();
            when(modelRepository.findByStatus(ModelDo.ModelStatus.ACTIVE)).thenReturn(List.of(doEntity));

            // when
            List<Model> result = gateway.findAllActive();

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getStatus()).isEqualTo(Model.ModelStatus.ACTIVE);
        }
    }

    @Nested
    @DisplayName("findByProviderId 方法测试")
    class FindByProviderIdTests {

        @Test
        @DisplayName("通过提供商 ID 找到 Model 列表")
        void findByProviderId_existingProviderId_returnsList() {
            // given
            ModelDo doEntity = createTestDo();
            when(modelRepository.findByProviderId(1L)).thenReturn(List.of(doEntity));

            // when
            List<Model> result = gateway.findByProviderId(1L);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getProviderId()).isEqualTo(1L);
        }
    }

    @Nested
    @DisplayName("count 方法测试")
    class CountTests {

        @Test
        @DisplayName("返回总数")
        void count_returnsCount() {
            // given
            when(modelRepository.count()).thenReturn(10L);

            // when
            long result = gateway.count();

            // then
            assertThat(result).isEqualTo(10L);
        }
    }

    @Nested
    @DisplayName("delete 方法测试")
    class DeleteTests {

        @Test
        @DisplayName("删除成功")
        void delete_existingEntity_deletes() {
            // given
            Model entity = createTestEntity();
            doNothing().when(modelRepository).delete(any());

            // when
            gateway.delete(entity);

            // then
            verify(modelRepository).delete(any());
        }
    }

    // Helper methods
    private Model createTestEntity() {
        Model entity = new Model();
        entity.setId(1L);
        entity.setProviderModelId("gpt-4");
        entity.setDisplayName("GPT-4");
        entity.setContextWindow(8192);
        entity.setInputPrice(BigDecimal.valueOf(0.03));
        entity.setOutputPrice(BigDecimal.valueOf(0.06));
        entity.setCapabilities(Map.of("chat", true, "streaming", true));
        entity.setStatus(Model.ModelStatus.ACTIVE);

        entity.setProviderId(1L);
        entity.setProviderName("OpenAI");

        return entity;
    }

    private ModelDo createTestDo() {
        ModelDo doEntity = new ModelDo();
        doEntity.setId(1L);
        doEntity.setProviderModelId("gpt-4");
        doEntity.setDisplayName("GPT-4");
        doEntity.setContextWindow(8192);
        doEntity.setInputPrice(BigDecimal.valueOf(0.03));
        doEntity.setOutputPrice(BigDecimal.valueOf(0.06));
        doEntity.setCapabilities(Map.of("chat", true, "streaming", true));
        doEntity.setStatus(ModelDo.ModelStatus.ACTIVE);
        doEntity.setCreatedAt(Instant.now());
        doEntity.setUpdatedAt(Instant.now());

        ProviderDo providerDo = new ProviderDo();
        providerDo.setId(1L);
        providerDo.setProviderName("OpenAI");
        providerDo.setProviderType(ProviderType.OPENAI);
        providerDo.setStatus(ProviderDo.ProviderStatus.ACTIVE);
        doEntity.setProvider(providerDo);

        return doEntity;
    }
}
