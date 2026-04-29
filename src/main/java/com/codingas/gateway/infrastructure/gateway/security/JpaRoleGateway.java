package com.codingas.gateway.infrastructure.gateway.security;

import com.codingas.gateway.domain.security.entity.Role;
import com.codingas.gateway.domain.security.entity.UserRole;
import com.codingas.gateway.domain.security.gateway.RoleGateway;
import com.codingas.gateway.infrastructure.security.RoleDo;
import com.codingas.gateway.infrastructure.security.RoleRepository;
import com.codingas.gateway.infrastructure.security.UserRoleDo;
import com.codingas.gateway.infrastructure.security.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 角色网关 JPA 实现
 *
 * <p>实现 RoleGateway 接口，负责 DO ↔ Entity 转换。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JpaRoleGateway implements RoleGateway {

    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;

    @Override
    public Role save(Role role) {
        RoleDo doEntity = toDo(role);
        RoleDo saved = roleRepository.save(doEntity);
        return toEntity(saved);
    }

    @Override
    public Optional<Role> findById(Long id) {
        return roleRepository.findById(id).map(this::toEntity);
    }

    @Override
    public Optional<Role> findByRoleCode(String roleCode) {
        return roleRepository.findByRoleCode(roleCode).map(this::toEntity);
    }

    @Override
    public List<Role> findAll() {
        return roleRepository.findAll().stream()
            .map(this::toEntity)
            .collect(Collectors.toList());
    }

    @Override
    public List<Role> findByRoleCodes(List<String> roleCodes) {
        return roleRepository.findByRoleCodeIn(roleCodes).stream()
            .map(this::toEntity)
            .collect(Collectors.toList());
    }

    @Override
    public long count() {
        return roleRepository.count();
    }

    @Override
    public void delete(Role role) {
        roleRepository.delete(toDo(role));
    }

    @Override
    public boolean existsByRoleCode(String roleCode) {
        return roleRepository.existsByRoleCode(roleCode);
    }

    @Override
    public void saveUserRoles(Long userId, List<Long> roleIds) {
        // 删除现有用户角色关联
        userRoleRepository.deleteByUserId(userId);
        // 创建新的用户角色关联
        for (Long roleId : roleIds) {
            UserRoleDo userRoleDo = new UserRoleDo();
            // User 和 Role 关联暂不处理简化处理
            userRoleRepository.save(userRoleDo);
        }
    }

    /**
     * DO 转 Entity
     */
    private Role toEntity(RoleDo doEntity) {
        if (doEntity == null) {
            return null;
        }
        Role entity = new Role();
        entity.setId(doEntity.getId());
        entity.setRoleCode(doEntity.getRoleCode());
        entity.setName(doEntity.getName());
        entity.setDescription(doEntity.getDescription());
        entity.setIsActive(doEntity.getIsActive());
        entity.setDeletedAt(doEntity.getDeletedAt());
        entity.setCreatedAt(doEntity.getCreatedAt());
        entity.setUpdatedAt(doEntity.getUpdatedAt());
        // 枚举转换
        if (doEntity.getRoleType() != null) {
            entity.setRoleType(Role.RoleType.valueOf(doEntity.getRoleType().name()));
        }
        return entity;
    }

    /**
     * Entity 转 DO
     */
    private RoleDo toDo(Role entity) {
        if (entity == null) {
            return null;
        }
        RoleDo doEntity = new RoleDo();
        if (entity.getId() != null) {
            doEntity.setId(entity.getId());
        }
        doEntity.setRoleCode(entity.getRoleCode());
        doEntity.setName(entity.getName());
        doEntity.setDescription(entity.getDescription());
        doEntity.setIsActive(entity.getIsActive());
        doEntity.setDeletedAt(entity.getDeletedAt());
        // 枚举转换
        if (entity.getRoleType() != null) {
            doEntity.setRoleType(RoleDo.RoleType.valueOf(entity.getRoleType().name()));
        }
        return doEntity;
    }
}