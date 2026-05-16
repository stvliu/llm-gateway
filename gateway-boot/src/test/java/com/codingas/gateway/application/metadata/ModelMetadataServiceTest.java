package com.codingas.gateway.application.metadata;

import com.codingas.gateway.application.metadata.dto.ModelMetadataResponse;
import com.codingas.gateway.domain.metadata.entity.MetadataSource;
import com.codingas.gateway.domain.metadata.entity.ModelMetadata;
import com.codingas.gateway.domain.metadata.enums.MetadataState;
import com.codingas.gateway.domain.metadata.gateway.ModelMetadataGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 模型元数据服务单元测试
 */
@ExtendWith(MockitoExtension.class)
class ModelMetadataServiceTest {

    @Mock
    private ModelMetadataGateway modelMetadataGateway;

    private ModelMetadataService service;

    @BeforeEach
    void setUp() {
        service = new ModelMetadataService(modelMetadataGateway);
    }

    @Nested
    @DisplayName("获取模型元数据详情")
    class GetTests {

        @Test
        @DisplayName("存在时返回详情")
        void existingId_returnsDetail() {
            ModelMetadata metadata = buildModelMetadata(1L, "openai", "gpt-4.1", "GPT-4.1");
            when(modelMetadataGateway.findById(1L)).thenReturn(Optional.of(metadata));

            ModelMetadataResponse response = service.getModelMetadata(1L);

            assertThat(response.getProviderId()).isEqualTo("openai");
            assertThat(response.getProviderModelId()).isEqualTo("gpt-4.1");
            assertThat(response.getDisplayName()).isEqualTo("GPT-4.1");
        }

        @Test
        @DisplayName("不存在时抛出异常")
        void nonExistingId_throwsException() {
            when(modelMetadataGateway.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getModelMetadata(99L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("99");
        }
    }

    @Nested
    @DisplayName("按供应商查询模型")
    class FindByProviderTests {

        @Test
        @DisplayName("返回指定供应商的所有模型")
        void returnsModelsForProvider() {
            ModelMetadata m1 = buildModelMetadata(1L, "openai", "gpt-4.1", "GPT-4.1");
            ModelMetadata m2 = buildModelMetadata(2L, "openai", "gpt-4.1-mini", "GPT-4.1 Mini");

            when(modelMetadataGateway.findByProviderId("openai")).thenReturn(List.of(m1, m2));

            List<ModelMetadataResponse> result = service.listByProviderId("openai");

            assertThat(result).hasSize(2);
            assertThat(result.stream().map(ModelMetadataResponse::getProviderModelId))
                .containsExactly("gpt-4.1", "gpt-4.1-mini");
        }

        @Test
        @DisplayName("供应商无模型时返回空列表")
        void noModels_returnsEmptyList() {
            when(modelMetadataGateway.findByProviderId("unknown")).thenReturn(List.of());

            List<ModelMetadataResponse> result = service.listByProviderId("unknown");

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("创建模型元数据")
    class CreateTests {

        @Test
        @DisplayName("创建成功")
        void createsSuccessfully() {
            ModelMetadata metadata = new ModelMetadata("openai", "gpt-4.1", "GPT-4.1", MetadataSource.MANUAL);
            metadata.setContextWindow(1047576);
            metadata.setInputPrice(BigDecimal.valueOf(2.0));
            metadata.setOutputPrice(BigDecimal.valueOf(8.0));

            when(modelMetadataGateway.existsByProviderIdAndModelId("openai", "gpt-4.1"))
                .thenReturn(false);
            when(modelMetadataGateway.save(any(ModelMetadata.class)))
                .thenAnswer(inv -> {
                    ModelMetadata m = inv.getArgument(0);
                    m.setId(1L);
                    return m;
                });

            ModelMetadataResponse response = service.createModelMetadata(metadata);

            assertThat(response.getProviderId()).isEqualTo("openai");
            assertThat(response.getProviderModelId()).isEqualTo("gpt-4.1");
            assertThat(response.getContextWindow()).isEqualTo(1047576);
            verify(modelMetadataGateway).save(any(ModelMetadata.class));
        }

        @Test
        @DisplayName("重复 providerId+modelId 时抛出异常")
        void duplicateKey_throwsException() {
            ModelMetadata metadata = new ModelMetadata("openai", "gpt-4.1", "GPT-4.1", MetadataSource.MANUAL);

            when(modelMetadataGateway.existsByProviderIdAndModelId("openai", "gpt-4.1"))
                .thenReturn(true);

            assertThatThrownBy(() -> service.createModelMetadata(metadata))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("gpt-4.1");
            verify(modelMetadataGateway, never()).save(any());
        }
    }

    @Nested
    @DisplayName("更新模型元数据")
    class UpdateTests {

        @Test
        @DisplayName("存在时更新成功")
        void existingId_updatesSuccessfully() {
            ModelMetadata existing = buildModelMetadata(1L, "openai", "gpt-4.1", "GPT-4.1");
            ModelMetadata update = new ModelMetadata();
            update.setDisplayName("GPT-4.1 Updated");
            update.setContextWindow(2000000);

            when(modelMetadataGateway.findById(1L)).thenReturn(Optional.of(existing));
            when(modelMetadataGateway.save(any(ModelMetadata.class)))
                .thenAnswer(inv -> inv.getArgument(0));

            ModelMetadataResponse response = service.updateModelMetadata(1L, update);

            assertThat(response.getDisplayName()).isEqualTo("GPT-4.1 Updated");
            assertThat(response.getContextWindow()).isEqualTo(2000000);
        }

        @Test
        @DisplayName("不存在时抛出异常")
        void nonExistingId_throwsException() {
            ModelMetadata update = new ModelMetadata();
            when(modelMetadataGateway.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.updateModelMetadata(99L, update))
                .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("删除模型元数据")
    class DeleteTests {

        @Test
        @DisplayName("调用 gateway 删除")
        void deletes_callsGateway() {
            service.deleteModelMetadata(1L);
            verify(modelMetadataGateway).deleteById(1L);
        }
    }

    private ModelMetadata buildModelMetadata(Long id, String providerId, String modelId, String displayName) {
        ModelMetadata metadata = new ModelMetadata(providerId, modelId, displayName, MetadataSource.BUILTIN);
        metadata.setId(id);
        metadata.setState(MetadataState.ACTIVE);
        return metadata;
    }
}