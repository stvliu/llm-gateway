package com.codingas.gateway.application.model;

import com.codingas.gateway.application.model.dto.ModelDiscoveryResponse;
import com.codingas.gateway.domain.iam.entity.UserApiKey;
import com.codingas.gateway.domain.iam.gateway.UserApiKeyGateway;
import com.codingas.gateway.common.exception.GatewayRequestException;
import com.codingas.gateway.domain.supply.entity.ChannelModel;
import com.codingas.gateway.domain.supply.entity.Model;
import com.codingas.gateway.domain.supply.enums.ChannelModelState;
import com.codingas.gateway.domain.supply.enums.ModelState;
import com.codingas.gateway.domain.supply.gateway.ChannelModelGateway;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * ModelDiscoveryService 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ModelDiscoveryService 单元测试")
class ModelDiscoveryServiceTest {

    @Mock
    private UserApiKeyGateway userApiKeyGateway;
    @Mock
    private ChannelModelGateway channelModelGateway;
    @Mock
    private ModelGateway modelGateway;

    private ModelDiscoveryService service;

    @BeforeEach
    void setUp() {
        service = new ModelDiscoveryService(userApiKeyGateway, channelModelGateway, modelGateway);
    }

    @Nested
    @DisplayName("getVisibleModels 获取可见模型列表")
    class GetVisibleModelsTests {

        @Test
        @DisplayName("返回 API Key 渠道关联的所有活跃模型")
        void shouldReturnModelsVisibleToApiKey() {
            // Arrange
            UserApiKey apiKey = new UserApiKey();
            apiKey.setId(1L);
            apiKey.setChannelIds(List.of(10L, 20L));

            when(userApiKeyGateway.findById(1L)).thenReturn(Optional.of(apiKey));

            ChannelModel cm1 = new ChannelModel();
            cm1.setModelId(100L);
            cm1.setState(ChannelModelState.ACTIVE);

            ChannelModel cm2 = new ChannelModel();
            cm2.setModelId(200L);
            cm2.setState(ChannelModelState.ACTIVE);

            when(channelModelGateway.findActiveByChannelId(10L)).thenReturn(List.of(cm1));
            when(channelModelGateway.findActiveByChannelId(20L)).thenReturn(List.of(cm2));

            Model model1 = new Model();
            model1.setModelName("gpt-4");
            model1.setState(ModelState.ACTIVE);
            model1.setCreatedAt(Instant.ofEpochSecond(1700000000L));

            Model model2 = new Model();
            model2.setModelName("gpt-3.5-turbo");
            model2.setState(ModelState.ACTIVE);
            model2.setCreatedAt(Instant.ofEpochSecond(1700000001L));

            when(modelGateway.findById(100L)).thenReturn(Optional.of(model1));
            when(modelGateway.findById(200L)).thenReturn(Optional.of(model2));

            // Act
            ModelDiscoveryResponse response = service.getVisibleModels(1L);

            // Assert
            assertThat(response.getObject()).isEqualTo("list");
            assertThat(response.getData()).hasSize(2);
            assertThat(response.getData().get(0).getId()).isEqualTo("gpt-4");
            assertThat(response.getData().get(1).getId()).isEqualTo("gpt-3.5-turbo");
        }

        @Test
        @DisplayName("API Key 没有关联渠道时返回空列表")
        void shouldReturnEmptyListWhenNoChannels() {
            UserApiKey apiKey = new UserApiKey();
            apiKey.setId(2L);
            apiKey.setChannelIds(List.of());

            when(userApiKeyGateway.findById(2L)).thenReturn(Optional.of(apiKey));

            ModelDiscoveryResponse response = service.getVisibleModels(2L);

            assertThat(response.getData()).isEmpty();
        }

        @Test
        @DisplayName("API Key 的 channelIds 为 null 时返回空列表")
        void shouldReturnEmptyListWhenChannelIdsNull() {
            UserApiKey apiKey = new UserApiKey();
            apiKey.setId(3L);
            apiKey.setChannelIds(null);

            when(userApiKeyGateway.findById(3L)).thenReturn(Optional.of(apiKey));

            ModelDiscoveryResponse response = service.getVisibleModels(3L);

            assertThat(response.getData()).isEmpty();
        }

        @Test
        @DisplayName("仅返回状态为 ACTIVE 的模型，过滤掉 INACTIVE 的模型")
        void shouldFilterInactiveModels() {
            UserApiKey apiKey = new UserApiKey();
            apiKey.setId(4L);
            apiKey.setChannelIds(List.of(30L));

            when(userApiKeyGateway.findById(4L)).thenReturn(Optional.of(apiKey));

            ChannelModel cm = new ChannelModel();
            cm.setModelId(300L);
            cm.setState(ChannelModelState.ACTIVE);

            when(channelModelGateway.findActiveByChannelId(30L)).thenReturn(List.of(cm));

            Model inactiveModel = new Model();
            inactiveModel.setModelName("deprecated-model");
            inactiveModel.setState(ModelState.INACTIVE);

            when(modelGateway.findById(300L)).thenReturn(Optional.of(inactiveModel));

            ModelDiscoveryResponse response = service.getVisibleModels(4L);

            assertThat(response.getData()).isEmpty();
        }

        @Test
        @DisplayName("模型不存在时（findById 返回空）直接跳过")
        void shouldSkipWhenModelNotFound() {
            UserApiKey apiKey = new UserApiKey();
            apiKey.setId(5L);
            apiKey.setChannelIds(List.of(40L));

            when(userApiKeyGateway.findById(5L)).thenReturn(Optional.of(apiKey));
            when(channelModelGateway.findActiveByChannelId(40L)).thenReturn(List.of(new ChannelModel() {{
                setModelId(999L);
                setState(ChannelModelState.ACTIVE);
            }}));
            when(modelGateway.findById(999L)).thenReturn(Optional.empty());

            ModelDiscoveryResponse response = service.getVisibleModels(5L);

            assertThat(response.getData()).isEmpty();
        }

        @Test
        @DisplayName("API Key 不存在时抛出 GatewayRequestException")
        void shouldThrowWhenApiKeyNotFound() {
            when(userApiKeyGateway.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getVisibleModels(99L))
                    .isInstanceOf(GatewayRequestException.class)
                    .hasMessageContaining("API Key 不存在");
        }
    }
}