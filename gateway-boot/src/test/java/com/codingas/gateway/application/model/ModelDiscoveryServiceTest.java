package com.codingas.gateway.application.model;

import com.codingas.gateway.application.model.dto.ModelDiscoveryResponse;
import com.codingas.gateway.common.exception.GatewayRequestException;
import com.codingas.gateway.domain.supply.entity.ModelInstance;
import com.codingas.gateway.domain.supply.entity.Model;
import com.codingas.gateway.domain.supply.enums.ChannelModelState;
import com.codingas.gateway.domain.supply.enums.ModelState;
import com.codingas.gateway.domain.supply.gateway.ModelInstanceGateway;
import com.codingas.gateway.domain.supply.gateway.ModelGateway;
import com.codingas.gateway.domain.team.gateway.TeamChannelGateway;
import com.codingas.gateway.domain.team.gateway.UserTeamGateway;
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
    private UserTeamGateway userTeamGateway;
    @Mock
    private TeamChannelGateway teamChannelGateway;
    @Mock
    private ModelInstanceGateway modelInstanceGateway;
    @Mock
    private ModelGateway modelGateway;

    private ModelDiscoveryService service;

    private static final Long USER_ID = 1L;
    private static final Long TEAM_ID = 100L;

    @BeforeEach
    void setUp() {
        service = new ModelDiscoveryService(userTeamGateway, teamChannelGateway, modelInstanceGateway, modelGateway);
    }

    @Nested
    @DisplayName("getVisibleModels 获取可见模型列表")
    class GetVisibleModelsTests {

        @Test
        @DisplayName("返回用户团队渠道关联的所有活跃模型")
        void shouldReturnModelsVisibleToUser() {
            // Arrange
            when(userTeamGateway.findTeamIdByUserId(USER_ID)).thenReturn(TEAM_ID);
            when(teamChannelGateway.findChannelIdsByTeamId(TEAM_ID)).thenReturn(List.of(10L, 20L));

            ModelInstance mi1 = new ModelInstance();
            mi1.setModelId(100L);
            mi1.setState(ChannelModelState.ACTIVE);

            ModelInstance mi2 = new ModelInstance();
            mi2.setModelId(200L);
            mi2.setState(ChannelModelState.ACTIVE);

            when(modelInstanceGateway.findActiveByChannelId(10L)).thenReturn(List.of(mi1));
            when(modelInstanceGateway.findActiveByChannelId(20L)).thenReturn(List.of(mi2));

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
            ModelDiscoveryResponse response = service.getVisibleModels(USER_ID);

            // Assert
            assertThat(response.getObject()).isEqualTo("list");
            assertThat(response.getData()).hasSize(2);
            assertThat(response.getData().get(0).getId()).isEqualTo("gpt-4");
            assertThat(response.getData().get(1).getId()).isEqualTo("gpt-3.5-turbo");
        }

        @Test
        @DisplayName("用户未关联团队时返回空列表")
        void shouldReturnEmptyListWhenNoTeam() {
            when(userTeamGateway.findTeamIdByUserId(USER_ID)).thenReturn(null);

            ModelDiscoveryResponse response = service.getVisibleModels(USER_ID);

            assertThat(response.getData()).isEmpty();
        }

        @Test
        @DisplayName("团队未关联渠道时返回空列表")
        void shouldReturnEmptyListWhenNoChannels() {
            when(userTeamGateway.findTeamIdByUserId(USER_ID)).thenReturn(TEAM_ID);
            when(teamChannelGateway.findChannelIdsByTeamId(TEAM_ID)).thenReturn(List.of());

            ModelDiscoveryResponse response = service.getVisibleModels(USER_ID);

            assertThat(response.getData()).isEmpty();
        }

        @Test
        @DisplayName("仅返回状态为 ACTIVE 的模型，过滤掉 INACTIVE 的模型")
        void shouldFilterInactiveModels() {
            // Arrange
            when(userTeamGateway.findTeamIdByUserId(USER_ID)).thenReturn(TEAM_ID);
            when(teamChannelGateway.findChannelIdsByTeamId(TEAM_ID)).thenReturn(List.of(30L));

            ModelInstance mi = new ModelInstance();
            mi.setModelId(300L);
            mi.setState(ChannelModelState.ACTIVE);

            when(modelInstanceGateway.findActiveByChannelId(30L)).thenReturn(List.of(mi));

            Model inactiveModel = new Model();
            inactiveModel.setModelName("deprecated-model");
            inactiveModel.setState(ModelState.INACTIVE);

            when(modelGateway.findById(300L)).thenReturn(Optional.of(inactiveModel));

            // Act
            ModelDiscoveryResponse response = service.getVisibleModels(USER_ID);

            // Assert
            assertThat(response.getData()).isEmpty();
        }

        @Test
        @DisplayName("模型不存在时（findById 返回空）直接跳过")
        void shouldSkipWhenModelNotFound() {
            // Arrange
            when(userTeamGateway.findTeamIdByUserId(USER_ID)).thenReturn(TEAM_ID);
            when(teamChannelGateway.findChannelIdsByTeamId(TEAM_ID)).thenReturn(List.of(40L));

            ModelInstance mi = new ModelInstance();
            mi.setModelId(999L);
            mi.setState(ChannelModelState.ACTIVE);

            when(modelInstanceGateway.findActiveByChannelId(40L)).thenReturn(List.of(mi));
            when(modelGateway.findById(999L)).thenReturn(Optional.empty());

            // Act
            ModelDiscoveryResponse response = service.getVisibleModels(USER_ID);

            // Assert
            assertThat(response.getData()).isEmpty();
        }
    }
}
