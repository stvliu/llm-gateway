package com.codingas.gateway.domain.proxy.service;

import com.codingas.gateway.domain.proxy.entity.RouteGroup;
import com.codingas.gateway.domain.proxy.gateway.RouteGroupGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * RoutingDomainService 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RoutingDomainService 测试")
class RoutingDomainServiceTest {

    @Mock
    private RouteGroupGateway routeGroupGateway;

    private RoutingDomainService service;

    @BeforeEach
    void setUp() {
        service = new RoutingDomainService(routeGroupGateway);
    }

    @Nested
    @DisplayName("findById 测试")
    class FindByIdTests {

        @Test
        @DisplayName("根据 ID 查找路由分组")
        void findById_found() {
            // Given
            RouteGroup group = createTestRouteGroup(1L, true);
            when(routeGroupGateway.findById(1L)).thenReturn(group);

            // When
            Optional<RouteGroup> result = service.findById(1L);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("找不到分组返回空")
        void findById_notFound() {
            // Given
            when(routeGroupGateway.findById(999L)).thenReturn(null);

            // When
            Optional<RouteGroup> result = service.findById(999L);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("null 参数返回空")
        void findById_nullId() {
            // When
            Optional<RouteGroup> result = service.findById(null);

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findAllActive 测试")
    class FindAllActiveTests {

        @Test
        @DisplayName("查找所有活跃分组")
        void findAllActive() {
            // Given
            RouteGroup group1 = createTestRouteGroup(1L, true);
            RouteGroup group2 = createTestRouteGroup(2L, true);
            when(routeGroupGateway.findAllActive()).thenReturn(List.of(group1, group2));

            // When
            List<RouteGroup> result = service.findAllActive();

            // Then
            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("无活跃分组返回空列表")
        void findAllActive_empty() {
            // Given
            when(routeGroupGateway.findAllActive()).thenReturn(List.of());

            // When
            List<RouteGroup> result = service.findAllActive();

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("isRouteGroupAvailable 测试")
    class IsRouteGroupAvailableTests {

        @Test
        @DisplayName("分组存在且启用返回 true")
        void isRouteGroupAvailable_enabled() {
            // Given
            RouteGroup group = createTestRouteGroup(1L, true);
            when(routeGroupGateway.findById(1L)).thenReturn(group);

            // When
            boolean result = service.isRouteGroupAvailable(1L);

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("分组存在但未启用返回 false")
        void isRouteGroupAvailable_disabled() {
            // Given
            RouteGroup group = createTestRouteGroup(1L, false);
            when(routeGroupGateway.findById(1L)).thenReturn(group);

            // When
            boolean result = service.isRouteGroupAvailable(1L);

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("分组不存在返回 false")
        void isRouteGroupAvailable_notFound() {
            // Given
            when(routeGroupGateway.findById(999L)).thenReturn(null);

            // When
            boolean result = service.isRouteGroupAvailable(999L);

            // Then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("getRoutingStrategy 测试")
    class GetRoutingStrategyTests {

        @Test
        @DisplayName("获取分组路由策略")
        void getRoutingStrategy_found() {
            // Given
            RouteGroup group = createTestRouteGroup(1L, true);
            group.setStrategy(RouteGroup.RoutingStrategy.FAILOVER);
            when(routeGroupGateway.findById(1L)).thenReturn(group);

            // When
            RouteGroup.RoutingStrategy result = service.getRoutingStrategy(1L);

            // Then
            assertThat(result).isEqualTo(RouteGroup.RoutingStrategy.FAILOVER);
        }

        @Test
        @DisplayName("分组不存在返回默认策略")
        void getRoutingStrategy_notFound_returnsDefault() {
            // Given
            when(routeGroupGateway.findById(999L)).thenReturn(null);

            // When
            RouteGroup.RoutingStrategy result = service.getRoutingStrategy(999L);

            // Then
            assertThat(result).isEqualTo(RouteGroup.RoutingStrategy.WEIGHTED);
        }

        @Test
        @DisplayName("分组未启用返回默认策略")
        void getRoutingStrategy_disabled_returnsDefault() {
            // Given
            RouteGroup group = createTestRouteGroup(1L, false);
            group.setStrategy(RouteGroup.RoutingStrategy.FAILOVER);
            when(routeGroupGateway.findById(1L)).thenReturn(group);

            // When
            RouteGroup.RoutingStrategy result = service.getRoutingStrategy(1L);

            // Then
            assertThat(result).isEqualTo(RouteGroup.RoutingStrategy.WEIGHTED);
        }
    }

    @Nested
    @DisplayName("save 测试")
    class SaveTests {

        @Test
        @DisplayName("保存路由分组")
        void save() {
            // Given
            RouteGroup group = createTestRouteGroup(1L, true);
            when(routeGroupGateway.save(any())).thenReturn(group);

            // When
            RouteGroup result = service.save(group);

            // Then
            assertThat(result).isEqualTo(group);
            verify(routeGroupGateway).save(group);
        }
    }

    // Helper methods
    private RouteGroup createTestRouteGroup(Long id, boolean enabled) {
        RouteGroup group = new RouteGroup();
        group.setId(id);
        group.setEnabled(enabled);
        group.setStrategy(RouteGroup.RoutingStrategy.WEIGHTED);
        return group;
    }
}
