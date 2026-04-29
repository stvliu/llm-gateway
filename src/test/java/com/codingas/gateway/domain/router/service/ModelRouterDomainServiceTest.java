package com.codingas.gateway.domain.router.service;

import com.codingas.gateway.domain.router.entity.Model;
import com.codingas.gateway.domain.router.entity.RouteGroup;
import com.codingas.gateway.domain.router.gateway.ModelGateway;
import com.codingas.gateway.domain.router.gateway.RouteGroupGateway;
import com.codingas.gateway.infrastructure.config.GatewayProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * ModelRouterDomainService 单元测试
 */
@ExtendWith(MockitoExtension.class)
class ModelRouterDomainServiceTest {

    @Mock
    private ModelGateway modelGateway;

    @Mock
    private RouteGroupGateway routeGroupGateway;

    @Mock
    private GatewayProperties properties;

    @Mock
    private GatewayProperties.RouterProperties routerProperties;

    @InjectMocks
    private ModelRouterDomainService modelRouterService;

    private Model activeModel;
    private Model inactiveModel;

    @BeforeEach
    void setUp() {
        activeModel = new Model();
        activeModel.setId(1L);
        activeModel.setModelCode("openai/gpt-4o");
        activeModel.setStatus(Model.ModelStatus.ACTIVE);

        inactiveModel = new Model();
        inactiveModel.setId(2L);
        inactiveModel.setModelCode("openai/gpt-3.5");
        inactiveModel.setStatus(Model.ModelStatus.DELETED);
    }

    @Nested
    @DisplayName("selectModel(String modelCode) 测试")
    class SelectModelTests {

        @Test
        @DisplayName("当模型存在且活跃时，返回该模型")
        void selectModel_existingActiveModel_returnsModel() {
            // given
            when(modelGateway.findByModelCode("openai/gpt-4o"))
                .thenReturn(Optional.of(activeModel));

            // when
            Model result = modelRouterService.selectModel("openai/gpt-4o");

            // then
            assertThat(result).isEqualTo(activeModel);
            verify(modelGateway).findByModelCode("openai/gpt-4o");
        }

        @Test
        @DisplayName("当模型代码为空时，选择默认模型")
        void selectModel_nullModelCode_selectsDefaultModel() {
            // given
            when(properties.getRouter()).thenReturn(routerProperties);
            when(routerProperties.getDefaultModelCode()).thenReturn("openai/gpt-4o");
            when(modelGateway.findByModelCode("openai/gpt-4o"))
                .thenReturn(Optional.of(activeModel));

            // when
            Model result = modelRouterService.selectModel(null);

            // then
            assertThat(result).isEqualTo(activeModel);
        }

        @Test
        @DisplayName("当模型代码为空字符串时，选择默认模型")
        void selectModel_blankModelCode_selectsDefaultModel() {
            // given
            when(properties.getRouter()).thenReturn(routerProperties);
            when(routerProperties.getDefaultModelCode()).thenReturn("openai/gpt-4o");
            when(modelGateway.findByModelCode("openai/gpt-4o"))
                .thenReturn(Optional.of(activeModel));

            // when
            Model result = modelRouterService.selectModel("   ");

            // then
            assertThat(result).isEqualTo(activeModel);
        }

        @Test
        @DisplayName("当模型不存在时，选择默认模型")
        void selectModel_modelNotFound_fallsBackToDefault() {
            // given
            when(modelGateway.findByModelCode("unknown/model"))
                .thenReturn(Optional.empty());
            when(properties.getRouter()).thenReturn(routerProperties);
            when(routerProperties.getDefaultModelCode()).thenReturn("openai/gpt-4o");
            when(modelGateway.findByModelCode("openai/gpt-4o"))
                .thenReturn(Optional.of(activeModel));

            // when
            Model result = modelRouterService.selectModel("unknown/model");

            // then
            assertThat(result).isEqualTo(activeModel);
        }

        @Test
        @DisplayName("当模型不活跃时，选择默认模型")
        void selectModel_modelInactive_fallsBackToDefault() {
            // given
            when(modelGateway.findByModelCode("openai/gpt-3.5"))
                .thenReturn(Optional.of(inactiveModel));
            when(properties.getRouter()).thenReturn(routerProperties);
            when(routerProperties.getDefaultModelCode()).thenReturn("openai/gpt-4o");
            when(modelGateway.findByModelCode("openai/gpt-4o"))
                .thenReturn(Optional.of(activeModel));

            // when
            Model result = modelRouterService.selectModel("openai/gpt-3.5");

            // then
            assertThat(result).isEqualTo(activeModel);
        }
    }

    @Nested
    @DisplayName("selectDefaultModel() 测试")
    class SelectDefaultModelTests {

        @Test
        @DisplayName("当默认模型存在且活跃时，返回该模型")
        void selectDefaultModel_defaultModelExists_returnsModel() {
            // given
            when(properties.getRouter()).thenReturn(routerProperties);
            when(routerProperties.getDefaultModelCode()).thenReturn("openai/gpt-4o");
            when(modelGateway.findByModelCode("openai/gpt-4o"))
                .thenReturn(Optional.of(activeModel));

            // when
            Model result = modelRouterService.selectDefaultModel();

            // then
            assertThat(result).isEqualTo(activeModel);
        }

        @Test
        @DisplayName("当默认模型不存在时，返回第一个活跃模型")
        void selectDefaultModel_defaultNotFound_returnsFirstActiveModel() {
            // given
            when(properties.getRouter()).thenReturn(routerProperties);
            when(routerProperties.getDefaultModelCode()).thenReturn("openai/gpt-4o");
            when(modelGateway.findByModelCode("openai/gpt-4o"))
                .thenReturn(Optional.empty());
            when(modelGateway.findAllActive()).thenReturn(List.of(activeModel, inactiveModel));

            // when
            Model result = modelRouterService.selectDefaultModel();

            // then
            assertThat(result).isEqualTo(activeModel);
            verify(modelGateway).findAllActive();
        }

        @Test
        @DisplayName("当无活跃模型时，抛出 IllegalStateException")
        void selectDefaultModel_noActiveModels_throwsException() {
            // given
            when(properties.getRouter()).thenReturn(routerProperties);
            when(routerProperties.getDefaultModelCode()).thenReturn("openai/gpt-4o");
            when(modelGateway.findByModelCode("openai/gpt-4o"))
                .thenReturn(Optional.empty());
            when(modelGateway.findAllActive()).thenReturn(List.of());

            // when & then
            assertThatThrownBy(() -> modelRouterService.selectDefaultModel())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No active model available");
        }
    }

    @Nested
    @DisplayName("selectModelByRouteGroup(String groupCode, RoutingStrategy strategy) 测试")
    class SelectModelByRouteGroupTests {

        @Test
        @DisplayName("当路由分组存在且启用时，使用默认模型")
        void selectModelByRouteGroup_groupEnabled_usesDefaultModel() {
            // given
            RouteGroup group = new RouteGroup();
            group.setGroupCode("enterprise");
            group.setStrategy(RouteGroup.RoutingStrategy.COST_OPTIMIZED);
            group.setEnabled(true);

            when(routeGroupGateway.findByGroupCode("enterprise")).thenReturn(group);
            when(properties.getRouter()).thenReturn(routerProperties);
            when(routerProperties.getDefaultModelCode()).thenReturn("openai/gpt-4o");
            when(modelGateway.findByModelCode("openai/gpt-4o"))
                .thenReturn(Optional.of(activeModel));

            // when
            Model result = modelRouterService.selectModelByRouteGroup("enterprise", RouteGroup.RoutingStrategy.COST_OPTIMIZED);

            // then
            assertThat(result).isEqualTo(activeModel);
            verify(routeGroupGateway).findByGroupCode("enterprise");
        }

        @Test
        @DisplayName("当路由分组不存在时，使用默认模型")
        void selectModelByRouteGroup_groupNotFound_usesDefaultModel() {
            // given
            when(routeGroupGateway.findByGroupCode("unknown")).thenReturn(null);
            when(properties.getRouter()).thenReturn(routerProperties);
            when(routerProperties.getDefaultModelCode()).thenReturn("openai/gpt-4o");
            when(modelGateway.findByModelCode("openai/gpt-4o"))
                .thenReturn(Optional.of(activeModel));

            // when
            Model result = modelRouterService.selectModelByRouteGroup("unknown", RouteGroup.RoutingStrategy.RANDOM);

            // then
            assertThat(result).isEqualTo(activeModel);
        }

        @Test
        @DisplayName("当路由分组被禁用时，使用默认模型")
        void selectModelByRouteGroup_groupDisabled_usesDefaultModel() {
            // given
            RouteGroup group = new RouteGroup();
            group.setGroupCode("deprecated");
            group.setStrategy(RouteGroup.RoutingStrategy.WEIGHTED);
            group.setEnabled(false);

            when(routeGroupGateway.findByGroupCode("deprecated")).thenReturn(group);
            when(properties.getRouter()).thenReturn(routerProperties);
            when(routerProperties.getDefaultModelCode()).thenReturn("openai/gpt-4o");
            when(modelGateway.findByModelCode("openai/gpt-4o"))
                .thenReturn(Optional.of(activeModel));

            // when
            Model result = modelRouterService.selectModelByRouteGroup("deprecated", RouteGroup.RoutingStrategy.WEIGHTED);

            // then
            assertThat(result).isEqualTo(activeModel);
        }
    }

    @Nested
    @DisplayName("getAllActiveModels() 测试")
    class GetAllActiveModelsTests {

        @Test
        @DisplayName("返回所有活跃模型")
        void getAllActiveModels_returnsAllActiveModels() {
            // given
            List<Model> activeModels = List.of(activeModel, inactiveModel);
            when(modelGateway.findAllActive()).thenReturn(activeModels);

            // when
            List<Model> result = modelRouterService.getAllActiveModels();

            // then
            assertThat(result).hasSize(2);
            assertThat(result).isEqualTo(activeModels);
            verify(modelGateway).findAllActive();
        }

        @Test
        @DisplayName("当无活跃模型时，返回空列表")
        void getAllActiveModels_noActiveModels_returnsEmptyList() {
            // given
            when(modelGateway.findAllActive()).thenReturn(List.of());

            // when
            List<Model> result = modelRouterService.getAllActiveModels();

            // then
            assertThat(result).isEmpty();
            verify(modelGateway).findAllActive();
        }
    }
}