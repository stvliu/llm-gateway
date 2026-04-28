package com.codingas.gateway.application.role;

import com.codingas.gateway.adapter.admin.dto.role.RoleCreateRequest;
import com.codingas.gateway.adapter.admin.dto.role.RoleQueryRequest;
import com.codingas.gateway.adapter.admin.dto.role.RoleResponse;
import com.codingas.gateway.adapter.admin.dto.role.RoleUpdateRequest;
import com.codingas.gateway.common.dto.PageResponse;
import com.codingas.gateway.common.exception.DuplicateResourceException;
import com.codingas.gateway.common.exception.ResourceNotFoundException;
import com.codingas.gateway.domain.security.entity.Role;
import com.codingas.gateway.domain.security.gateway.RoleGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 角色应用服务
 *
 * <p>处理角色管理的业务逻辑。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoleApplication {

    private final RoleGateway roleGateway;

    /**
     * 创建角色
     */
    @Transactional
    public RoleResponse create(RoleCreateRequest request) {
        // 检查角色代码唯一性
        if (roleGateway.existsByRoleCode(request.getRoleCode())) {
            throw new DuplicateResourceException("Role", "roleCode");
        }

        // 创建角色
        Role role = new Role();
        role.setRoleCode(request.getRoleCode());
        role.setName(request.getName());
        role.setDescription(request.getDescription());
        role.setRoleType(request.getRoleType());
        role.setIsActive(true);

        Role savedRole = roleGateway.save(role);
        return toResponse(savedRole);
    }

    /**
     * 根据 ID 获取角色
     */
    public RoleResponse getById(Long id) {
        Role role = roleGateway.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Role", id));
        return toResponse(role);
    }

    /**
     * 查询角色列表
     */
    public PageResponse<RoleResponse> query(RoleQueryRequest request) {
        List<Role> roles = roleGateway.findAll();

        // 过滤
        if (request.getKeyword() != null && !request.getKeyword().isBlank()) {
            String keyword = request.getKeyword().toLowerCase();
            roles = roles.stream()
                .filter(r -> r.getRoleCode().toLowerCase().contains(keyword)
                    || r.getName().toLowerCase().contains(keyword))
                .collect(Collectors.toList());
        }

        if (request.getRoleType() != null) {
            roles = roles.stream()
                .filter(r -> r.getRoleType() == request.getRoleType())
                .collect(Collectors.toList());
        }

        if (request.getIsActive() != null) {
            roles = roles.stream()
                .filter(r -> r.getIsActive().equals(request.getIsActive()))
                .collect(Collectors.toList());
        }

        // 统计
        long total = roles.size();

        // 分页
        int offset = request.getOffset();
        int limit = request.getLimit();
        List<Role> pagedRoles = roles.stream()
            .skip(offset)
            .limit(limit)
            .collect(Collectors.toList());

        List<RoleResponse> responses = pagedRoles.stream()
            .map(this::toResponse)
            .collect(Collectors.toList());

        return PageResponse.of(responses, request.getPage(), limit, total);
    }

    /**
     * 更新角色
     */
    @Transactional
    public RoleResponse update(Long id, RoleUpdateRequest request) {
        Role role = roleGateway.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Role", id));

        if (request.getName() != null) {
            role.setName(request.getName());
        }
        if (request.getDescription() != null) {
            role.setDescription(request.getDescription());
        }
        if (request.getIsActive() != null) {
            role.setIsActive(request.getIsActive());
        }

        return toResponse(roleGateway.save(role));
    }

    /**
     * 删除角色（软删除）
     */
    @Transactional
    public void delete(Long id) {
        Role role = roleGateway.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Role", id));
        role.setDeletedAt(Instant.now());
        roleGateway.save(role);
    }

    /**
     * 启用/禁用角色
     */
    @Transactional
    public RoleResponse setEnabled(Long id, boolean enabled) {
        Role role = roleGateway.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Role", id));
        role.setIsActive(enabled);
        return toResponse(roleGateway.save(role));
    }

    /**
     * 转换为响应 DTO
     */
    private RoleResponse toResponse(Role role) {
        RoleResponse response = new RoleResponse();
        response.setId(role.getId());
        response.setRoleCode(role.getRoleCode());
        response.setName(role.getName());
        response.setDescription(role.getDescription());
        response.setRoleType(role.getRoleType());
        response.setIsActive(role.getIsActive());
        response.setCreatedAt(role.getCreatedAt());
        response.setUpdatedAt(role.getUpdatedAt());
        return response;
    }
}
