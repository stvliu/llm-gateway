package com.codingas.gateway.application.role;

import com.codingas.gateway.adapter.admin.dto.role.*;
import com.codingas.gateway.common.dto.PageResponse;
import com.codingas.gateway.common.exception.DuplicateResourceException;
import com.codingas.gateway.common.exception.ResourceNotFoundException;
import com.codingas.gateway.domain.security.entity.Role;
import com.codingas.gateway.domain.security.gateway.RoleGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * RoleApplication 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RoleApplication 单元测试")
class RoleApplicationTest {

    @Mock
    private RoleGateway roleGateway;

    @InjectMocks
    private RoleApplication roleApplication;

    private Role testRole;

    @BeforeEach
    void setUp() {
        testRole = createTestRole(1L, "ADMIN", "Administrator", Role.RoleType.SYSTEM);
    }

    // ==================== create 测试 ====================

    @Nested
    @DisplayName("create 创建角色")
    class CreateTests {

        @Test
        @DisplayName("创建角色成功")
        void create_validRequest_returnsRoleResponse() {
            // given
            RoleCreateRequest request = new RoleCreateRequest();
            request.setRoleCode("USER");
            request.setName("User Role");
            request.setDescription("Regular user role");
            request.setRoleType(Role.RoleType.CUSTOM);

            when(roleGateway.existsByRoleCode("USER")).thenReturn(false);
            when(roleGateway.save(any(Role.class))).thenAnswer(invocation -> {
                Role role = invocation.getArgument(0);
                role.setId(2L);
                return role;
            });

            // when
            RoleResponse response = roleApplication.create(request);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(2L);
            assertThat(response.getRoleCode()).isEqualTo("USER");
            assertThat(response.getName()).isEqualTo("User Role");
            assertThat(response.getDescription()).isEqualTo("Regular user role");
            assertThat(response.getRoleType()).isEqualTo(Role.RoleType.CUSTOM);
            assertThat(response.getIsActive()).isTrue();
            verify(roleGateway).existsByRoleCode("USER");
            verify(roleGateway).save(any(Role.class));
        }

        @Test
        @DisplayName("角色代码重复时抛出 DuplicateResourceException")
        void create_duplicateRoleCode_throwsException() {
            // given
            RoleCreateRequest request = new RoleCreateRequest();
            request.setRoleCode("ADMIN");
            request.setName("Admin Role");

            when(roleGateway.existsByRoleCode("ADMIN")).thenReturn(true);

            // when & then
            assertThatThrownBy(() -> roleApplication.create(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Role")
                .hasMessageContaining("roleCode");

            verify(roleGateway, never()).save(any(Role.class));
        }

        @Test
        @DisplayName("创建角色时默认启用")
        void create_validRequest_defaultIsActive() {
            // given
            RoleCreateRequest request = new RoleCreateRequest();
            request.setRoleCode("NEW_ROLE");
            request.setName("New Role");
            request.setRoleType(Role.RoleType.CUSTOM);

            when(roleGateway.existsByRoleCode("NEW_ROLE")).thenReturn(false);
            when(roleGateway.save(any(Role.class))).thenAnswer(invocation -> {
                Role role = invocation.getArgument(0);
                role.setId(2L);
                return role;
            });

            // when
            RoleResponse response = roleApplication.create(request);

            // then
            assertThat(response.getIsActive()).isTrue();
        }
    }

    // ==================== getById 测试 ====================

    @Nested
    @DisplayName("getById 获取角色")
    class GetByIdTests {

        @Test
        @DisplayName("角色存在时返回角色响应")
        void getById_existingRole_returnsRoleResponse() {
            // given
            when(roleGateway.findById(1L)).thenReturn(Optional.of(testRole));

            // when
            RoleResponse response = roleApplication.getById(1L);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(1L);
            assertThat(response.getRoleCode()).isEqualTo("ADMIN");
            assertThat(response.getName()).isEqualTo("Administrator");
            verify(roleGateway).findById(1L);
        }

        @Test
        @DisplayName("角色不存在时抛出 ResourceNotFoundException")
        void getById_nonExistingRole_throwsException() {
            // given
            when(roleGateway.findById(99L)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> roleApplication.getById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Role")
                .hasMessageContaining("99");
        }
    }

    // ==================== query 测试 ====================

    @Nested
    @DisplayName("query 查询角色列表")
    class QueryTests {

        @Test
        @DisplayName("返回所有角色")
        void query_noFilter_returnsAllRoles() {
            // given
            Role role2 = createTestRole(2L, "USER", "User Role", Role.RoleType.CUSTOM);
            when(roleGateway.findAll()).thenReturn(List.of(testRole, role2));

            RoleQueryRequest request = new RoleQueryRequest();
            request.setPage(1);
            request.setLimit(20);

            // when
            PageResponse<RoleResponse> response = roleApplication.query(request);

            // then
            assertThat(response.getItems()).hasSize(2);
            assertThat(response.getPagination().getTotal()).isEqualTo(2);
        }

        @Test
        @DisplayName("按关键字过滤角色")
        void query_withKeyword_filtersRoles() {
            // given
            when(roleGateway.findAll()).thenReturn(List.of(testRole));

            RoleQueryRequest request = new RoleQueryRequest();
            request.setKeyword("admin");
            request.setPage(1);
            request.setLimit(20);

            // when
            PageResponse<RoleResponse> response = roleApplication.query(request);

            // then
            assertThat(response.getItems()).hasSize(1);
            assertThat(response.getItems().get(0).getName()).isEqualTo("Administrator");
        }

        @Test
        @DisplayName("关键字匹配角色代码")
        void query_withKeywordMatchesRoleCode_filtersRoles() {
            // given
            when(roleGateway.findAll()).thenReturn(List.of(testRole));

            RoleQueryRequest request = new RoleQueryRequest();
            request.setKeyword("ADM");
            request.setPage(1);
            request.setLimit(20);

            // when
            PageResponse<RoleResponse> response = roleApplication.query(request);

            // then
            assertThat(response.getItems()).hasSize(1);
            assertThat(response.getItems().get(0).getRoleCode()).isEqualTo("ADMIN");
        }

        @Test
        @DisplayName("按角色类型过滤")
        void query_withRoleType_filtersRoles() {
            // given
            Role customRole = createTestRole(2L, "USER", "User Role", Role.RoleType.CUSTOM);
            when(roleGateway.findAll()).thenReturn(List.of(testRole, customRole));

            RoleQueryRequest request = new RoleQueryRequest();
            request.setRoleType(Role.RoleType.SYSTEM);
            request.setPage(1);
            request.setLimit(20);

            // when
            PageResponse<RoleResponse> response = roleApplication.query(request);

            // then
            assertThat(response.getItems()).hasSize(1);
            assertThat(response.getItems().get(0).getRoleType()).isEqualTo(Role.RoleType.SYSTEM);
        }

        @Test
        @DisplayName("按状态过滤")
        void query_withIsActive_filtersRoles() {
            // given
            Role inactiveRole = createTestRole(2L, "USER", "User Role", Role.RoleType.CUSTOM);
            inactiveRole.setIsActive(false);
            when(roleGateway.findAll()).thenReturn(List.of(testRole, inactiveRole));

            RoleQueryRequest request = new RoleQueryRequest();
            request.setIsActive(true);
            request.setPage(1);
            request.setLimit(20);

            // when
            PageResponse<RoleResponse> response = roleApplication.query(request);

            // then
            assertThat(response.getItems()).hasSize(1);
            assertThat(response.getItems().get(0).getIsActive()).isTrue();
        }

        @Test
        @DisplayName("分页查询")
        void query_withPagination_returnsPagedRoles() {
            // given
            List<Role> roles = new ArrayList<>();
            for (long i = 1; i <= 25; i++) {
                roles.add(createTestRole(i, "ROLE" + String.format("%03d", i), "Role " + i, Role.RoleType.CUSTOM));
            }
            when(roleGateway.findAll()).thenReturn(roles);

            RoleQueryRequest request = new RoleQueryRequest();
            request.setPage(2);
            request.setLimit(10);

            // when
            PageResponse<RoleResponse> response = roleApplication.query(request);

            // then
            assertThat(response.getItems()).hasSize(10);
            assertThat(response.getPagination().getPage()).isEqualTo(2);
            assertThat(response.getPagination().getLimit()).isEqualTo(10);
            assertThat(response.getPagination().getTotal()).isEqualTo(25);
            assertThat(response.getPagination().getTotalPages()).isEqualTo(3);
        }

        @Test
        @DisplayName("关键字不匹配时返回空列表")
        void query_noMatch_returnsEmptyList() {
            // given
            when(roleGateway.findAll()).thenReturn(List.of(testRole));

            RoleQueryRequest request = new RoleQueryRequest();
            request.setKeyword("nonexistent");
            request.setPage(1);
            request.setLimit(20);

            // when
            PageResponse<RoleResponse> response = roleApplication.query(request);

            // then
            assertThat(response.getItems()).isEmpty();
            assertThat(response.getPagination().getTotal()).isEqualTo(0);
        }

        @Test
        @DisplayName("组合过滤条件")
        void query_withMultipleFilters_combinesFilters() {
            // given
            Role customActiveRole = createTestRole(2L, "USER", "User Role", Role.RoleType.CUSTOM);
            customActiveRole.setIsActive(true);
            Role customInactiveRole = createTestRole(3L, "GUEST", "Guest Role", Role.RoleType.CUSTOM);
            customInactiveRole.setIsActive(false);
            when(roleGateway.findAll()).thenReturn(List.of(testRole, customActiveRole, customInactiveRole));

            RoleQueryRequest request = new RoleQueryRequest();
            request.setRoleType(Role.RoleType.CUSTOM);
            request.setIsActive(true);
            request.setPage(1);
            request.setLimit(20);

            // when
            PageResponse<RoleResponse> response = roleApplication.query(request);

            // then
            assertThat(response.getItems()).hasSize(1);
            assertThat(response.getItems().get(0).getRoleCode()).isEqualTo("USER");
        }
    }

    // ==================== update 测试 ====================

    @Nested
    @DisplayName("update 更新角色")
    class UpdateTests {

        @Test
        @DisplayName("更新角色名称成功")
        void update_validName_updatesRole() {
            // given
            when(roleGateway.findById(1L)).thenReturn(Optional.of(testRole));
            when(roleGateway.save(any(Role.class))).thenReturn(testRole);

            RoleUpdateRequest request = new RoleUpdateRequest();
            request.setName("Updated Admin");

            // when
            RoleResponse response = roleApplication.update(1L, request);

            // then
            assertThat(response).isNotNull();
            verify(roleGateway).findById(1L);
            verify(roleGateway).save(testRole);
        }

        @Test
        @DisplayName("更新角色描述成功")
        void update_validDescription_updatesRole() {
            // given
            when(roleGateway.findById(1L)).thenReturn(Optional.of(testRole));
            when(roleGateway.save(any(Role.class))).thenReturn(testRole);

            RoleUpdateRequest request = new RoleUpdateRequest();
            request.setDescription("Updated description");

            // when
            roleApplication.update(1L, request);

            // then
            verify(roleGateway).save(testRole);
        }

        @Test
        @DisplayName("更新角色状态成功")
        void update_validIsActive_updatesRole() {
            // given
            when(roleGateway.findById(1L)).thenReturn(Optional.of(testRole));
            when(roleGateway.save(any(Role.class))).thenAnswer(invocation -> invocation.getArgument(0));

            RoleUpdateRequest request = new RoleUpdateRequest();
            request.setIsActive(false);

            // when
            RoleResponse response = roleApplication.update(1L, request);

            // then
            assertThat(testRole.getIsActive()).isFalse();
            verify(roleGateway).save(testRole);
        }

        @Test
        @DisplayName("角色不存在时抛出异常")
        void update_nonExistingRole_throwsException() {
            // given
            when(roleGateway.findById(99L)).thenReturn(Optional.empty());

            RoleUpdateRequest request = new RoleUpdateRequest();
            request.setName("Updated Name");

            // when & then
            assertThatThrownBy(() -> roleApplication.update(99L, request))
                .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("更新时 null 字段不覆盖原有值")
        void update_nullFields_doesNotOverwrite() {
            // given
            String originalDescription = testRole.getDescription();
            when(roleGateway.findById(1L)).thenReturn(Optional.of(testRole));
            when(roleGateway.save(any(Role.class))).thenReturn(testRole);

            RoleUpdateRequest request = new RoleUpdateRequest();
            request.setName("Updated Name");
            // description 未设置，保持 null

            // when
            roleApplication.update(1L, request);

            // then
            assertThat(testRole.getDescription()).isEqualTo(originalDescription);
        }
    }

    // ==================== delete 测试 ====================

    @Nested
    @DisplayName("delete 删除角色")
    class DeleteTests {

        @Test
        @DisplayName("删除角色成功（软删除）")
        void delete_existingRole_softDeletes() {
            // given
            when(roleGateway.findById(1L)).thenReturn(Optional.of(testRole));
            when(roleGateway.save(any(Role.class))).thenReturn(testRole);

            // when
            roleApplication.delete(1L);

            // then
            assertThat(testRole.getDeletedAt()).isNotNull();
            verify(roleGateway).save(testRole);
        }

        @Test
        @DisplayName("删除不存在的角色抛出异常")
        void delete_nonExistingRole_throwsException() {
            // given
            when(roleGateway.findById(99L)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> roleApplication.delete(99L))
                .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ==================== setEnabled 测试 ====================

    @Nested
    @DisplayName("setEnabled 启用/禁用角色")
    class SetEnabledTests {

        @Test
        @DisplayName("启用角色成功")
        void setEnabled_enableRole_setsIsActiveTrue() {
            // given
            testRole.setIsActive(false);
            when(roleGateway.findById(1L)).thenReturn(Optional.of(testRole));
            when(roleGateway.save(any(Role.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // when
            RoleResponse response = roleApplication.setEnabled(1L, true);

            // then
            assertThat(testRole.getIsActive()).isTrue();
            assertThat(response.getIsActive()).isTrue();
            verify(roleGateway).save(testRole);
        }

        @Test
        @DisplayName("禁用角色成功")
        void setEnabled_disableRole_setsIsActiveFalse() {
            // given
            testRole.setIsActive(true);
            when(roleGateway.findById(1L)).thenReturn(Optional.of(testRole));
            when(roleGateway.save(any(Role.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // when
            RoleResponse response = roleApplication.setEnabled(1L, false);

            // then
            assertThat(testRole.getIsActive()).isFalse();
            assertThat(response.getIsActive()).isFalse();
            verify(roleGateway).save(testRole);
        }

        @Test
        @DisplayName("启用不存在的角色抛出异常")
        void setEnabled_nonExistingRole_throwsException() {
            // given
            when(roleGateway.findById(99L)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> roleApplication.setEnabled(99L, true))
                .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ==================== 辅助方法 ====================

    private Role createTestRole(Long id, String roleCode, String name, Role.RoleType roleType) {
        Role role = new Role();
        role.setId(id);
        role.setRoleCode(roleCode);
        role.setName(name);
        role.setDescription("Test role description");
        role.setRoleType(roleType);
        role.setIsActive(true);
        role.setCreatedAt(Instant.now());
        role.setUpdatedAt(Instant.now());
        return role;
    }
}
