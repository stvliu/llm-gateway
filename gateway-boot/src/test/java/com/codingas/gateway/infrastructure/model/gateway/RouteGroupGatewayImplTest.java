package com.codingas.gateway.infrastructure.model.gateway;

import com.codingas.gateway.domain.proxy.entity.RouteGroup;
import com.codingas.gateway.infrastructure.model.gateway.database.RouteGroupRepository;
import com.codingas.gateway.infrastructure.model.gateway.database.dataobject.RouteGroupDo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * RouteGroupGatewayImpl 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RouteGroupGatewayImpl 测试")
class RouteGroupGatewayImplTest {

    @Mock
    private RouteGroupRepository repository;

    @InjectMocks
    private RouteGroupGatewayImpl gateway;

    @Nested
    @DisplayName("findById 方法测试")
    class FindByIdTests {

        @Test
        @DisplayName("通过 ID 找到路由分组")
        void findById_existingId_returnsEntity() {
            // given
            RouteGroupDo doEntity = createTestDo();
            when(repository.findById(1L)).thenReturn(Optional.of(doEntity));

            // when
            RouteGroup result = gateway.findById(1L);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getGroupCode()).isEqualTo("group-001");
        }

        @Test
        @DisplayName("未找到返回 null")
        void findById_nonExistingId_returnsNull() {
            // given
            when(repository.findById(999L)).thenReturn(Optional.empty());

            // when
            RouteGroup result = gateway.findById(999L);

            // then
            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("findByGroupCode 方法测试")
    class FindByGroupCodeTests {

        @Test
        @DisplayName("通过编码找到路由分组")
        void findByGroupCode_existingCode_returnsEntity() {
            // given
            RouteGroupDo doEntity = createTestDo();
            when(repository.findByGroupCode("group-001")).thenReturn(Optional.of(doEntity));

            // when
            RouteGroup result = gateway.findByGroupCode("group-001");

            // then
            assertThat(result).isNotNull();
            assertThat(result.getGroupCode()).isEqualTo("group-001");
        }

        @Test
        @DisplayName("未找到返回 null")
        void findByGroupCode_nonExistingCode_returnsNull() {
            // given
            when(repository.findByGroupCode("unknown")).thenReturn(Optional.empty());

            // when
            RouteGroup result = gateway.findByGroupCode("unknown");

            // then
            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("findAllActive 方法测试")
    class FindAllActiveTests {

        @Test
        @DisplayName("返回所有启用的路由分组")
        void findAllActive_returnsActiveGroups() {
            // given
            RouteGroupDo doEntity = createTestDo();
            when(repository.findByEnabledTrue()).thenReturn(List.of(doEntity));

            // when
            List<RouteGroup> result = gateway.findAllActive();

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getEnabled()).isTrue();
        }
    }

    @Nested
    @DisplayName("save 方法测试")
    class SaveTests {

        @Test
        @DisplayName("保存路由分组成功")
        void save_validEntity_returnsSaved() {
            // given
            RouteGroup entity = createTestEntity();
            RouteGroupDo savedDo = createTestDo();

            when(repository.save(any())).thenReturn(savedDo);

            // when
            RouteGroup result = gateway.save(entity);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getGroupCode()).isEqualTo("group-001");
            verify(repository).save(any());
        }
    }

    // Helper methods
    private RouteGroup createTestEntity() {
        RouteGroup entity = new RouteGroup();
        entity.setId(1L);
        entity.setGroupCode("group-001");
        entity.setGroupName("Default Group");
        entity.setStrategy(RouteGroup.RoutingStrategy.WEIGHTED);
        entity.setEnabled(true);
        return entity;
    }

    private RouteGroupDo createTestDo() {
        RouteGroupDo doEntity = new RouteGroupDo();
        doEntity.setId(1L);
        doEntity.setGroupCode("group-001");
        doEntity.setGroupName("Default Group");
        doEntity.setStrategy(RouteGroupDo.RoutingStrategy.WEIGHTED);
        doEntity.setEnabled(true);
        doEntity.setCreatedAt(Instant.now());
        doEntity.setUpdatedAt(Instant.now());
        return doEntity;
    }
}
