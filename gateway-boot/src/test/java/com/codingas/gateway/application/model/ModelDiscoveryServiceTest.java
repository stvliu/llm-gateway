package com.codingas.gateway.application.model;

import com.codingas.gateway.application.model.dto.ModelDiscoveryResponse;
import com.codingas.gateway.domain.application.gateway.ApplicationChannelGateway;
import com.codingas.gateway.domain.supply.entity.Model;
import com.codingas.gateway.domain.supply.entity.ModelInstance;
import com.codingas.gateway.domain.supply.gateway.ModelInstanceGateway;
import com.codingas.gateway.domain.supply.gateway.ModelGateway;
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
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * ModelDiscoveryService 单元测试
 *
 * <p>D8：废弃团队模型可见性机制后，模型可见性由应用授权的渠道挂哪些 ModelInstance 隐式决定。
 * 本测试验证新契约：以应用 ID（权限锚点）查询应用授权渠道，再发现其上的活跃模型。</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ModelDiscoveryService 单元测试")
class ModelDiscoveryServiceTest {

    @Mock
    private ApplicationChannelGateway applicationChannelGateway;
    @Mock
    private ModelInstanceGateway modelInstanceGateway;
    @Mock
    private ModelGateway modelGateway;

    private ModelDiscoveryService service;

    private static final Long APPLICATION_ID = 100L;

    @BeforeEach
    void setUp() {
        service = new ModelDiscoveryService(applicationChannelGateway, modelInstanceGateway, modelGateway);
    }

    @Nested
    @DisplayName("getVisibleModels 获取可见模型列表")
    class GetVisibleModelsTests {

        @Test
        @DisplayName("返回应用授权渠道关联的所有活跃模型")
        void shouldReturnModelsVisibleToApplication() {
            // Arrange
            when(applicationChannelGateway.findChannelIdsByApplicationId(APPLICATION_ID))
                    .thenReturn(Set.of(10L, 20L));

            ModelInstance mi1 = new ModelInstance();
            mi1.setModelId(100L);
            mi1.setState(ModelInstance.State.ACTIVE);

            ModelInstance mi2 = new ModelInstance();
            mi2.setModelId(200L);
            mi2.setState(ModelInstance.State.ACTIVE);

            when(modelInstanceGateway.findActiveByChannelId(10L)).thenReturn(List.of(mi1));
            when(modelInstanceGateway.findActiveByChannelId(20L)).thenReturn(List.of(mi2));

            Model model1 = new Model();
            model1.setModelName("gpt-4");
            model1.setCreatedAt(Instant.ofEpochSecond(1700000000L));

            Model model2 = new Model();
            model2.setModelName("gpt-3.5-turbo");
            model2.setCreatedAt(Instant.ofEpochSecond(1700000001L));

            when(modelGateway.findById(100L)).thenReturn(Optional.of(model1));
            when(modelGateway.findById(200L)).thenReturn(Optional.of(model2));

            // Act
            ModelDiscoveryResponse response = service.getVisibleModels(APPLICATION_ID);

            // Assert（渠道来自 Set，顺序不保证，按任意序断言）
            assertThat(response.getObject()).isEqualTo("list");
            assertThat(response.getData()).hasSize(2);
            assertThat(response.getData()).extracting(ModelDiscoveryResponse.ModelItem::getId)
                    .containsExactlyInAnyOrder("gpt-4", "gpt-3.5-turbo");
        }

        @Test
        @DisplayName("应用 ID 为 null（无权限锚点）时返回空列表")
        void shouldReturnEmptyListWhenApplicationIdIsNull() {
            ModelDiscoveryResponse response = service.getVisibleModels(null);

            assertThat(response.getData()).isEmpty();
        }

        @Test
        @DisplayName("应用未授权任何渠道时返回空列表")
        void shouldReturnEmptyListWhenNoChannels() {
            when(applicationChannelGateway.findChannelIdsByApplicationId(APPLICATION_ID))
                    .thenReturn(Set.of());

            ModelDiscoveryResponse response = service.getVisibleModels(APPLICATION_ID);

            assertThat(response.getData()).isEmpty();
        }

        @Test
        @DisplayName("仅返回状态为可用（isAvailable）的模型，过滤掉已废弃的模型")
        void shouldFilterUnavailableModels() {
            // Arrange
            when(applicationChannelGateway.findChannelIdsByApplicationId(APPLICATION_ID))
                    .thenReturn(Set.of(30L));

            ModelInstance mi = new ModelInstance();
            mi.setModelId(300L);
            mi.setState(ModelInstance.State.ACTIVE);

            when(modelInstanceGateway.findActiveByChannelId(30L)).thenReturn(List.of(mi));

            Model inactiveModel = new Model();
            inactiveModel.setModelName("deprecated-model");
            inactiveModel.setDeprecatedAt(Instant.now());

            when(modelGateway.findById(300L)).thenReturn(Optional.of(inactiveModel));

            // Act
            ModelDiscoveryResponse response = service.getVisibleModels(APPLICATION_ID);

            // Assert
            assertThat(response.getData()).isEmpty();
        }

        @Test
        @DisplayName("模型不存在时（findById 返回空）直接跳过")
        void shouldSkipWhenModelNotFound() {
            // Arrange
            when(applicationChannelGateway.findChannelIdsByApplicationId(APPLICATION_ID))
                    .thenReturn(Set.of(40L));

            ModelInstance mi = new ModelInstance();
            mi.setModelId(999L);
            mi.setState(ModelInstance.State.ACTIVE);

            when(modelInstanceGateway.findActiveByChannelId(40L)).thenReturn(List.of(mi));
            when(modelGateway.findById(999L)).thenReturn(Optional.empty());

            // Act
            ModelDiscoveryResponse response = service.getVisibleModels(APPLICATION_ID);

            // Assert
            assertThat(response.getData()).isEmpty();
        }
    }
}
