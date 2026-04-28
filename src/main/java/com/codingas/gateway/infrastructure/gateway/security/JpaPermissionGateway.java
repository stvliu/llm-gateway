package com.codingas.gateway.infrastructure.gateway.security;

import com.codingas.gateway.domain.security.entity.Permission;
import com.codingas.gateway.domain.security.gateway.PermissionGateway;
import com.codingas.gateway.infrastructure.security.PermissionDo;
import com.codingas.gateway.infrastructure.security.PermissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 权限网关 JPA 实现
 *
 * <p>实现 PermissionGateway 接口，负责 DO ↔ Entity 转换。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JpaPermissionGateway implements PermissionGateway {

    private final PermissionRepository permissionRepository;

    @Override
    public Permission save(Permission permission) {
        PermissionDo doEntity = toDo(permission);
        PermissionDo saved = permissionRepository.save(doEntity);
        return toEntity(saved);
    }

    @Override
    public Optional<Permission> findById(Long id) {
        return permissionRepository.findById(id).map(this::toEntity);
    }

    @Override
    public Optional<Permission> findByPermissionCode(String permissionCode) {
        return permissionRepository.findByPermissionCode(permissionCode).map(this::toEntity);
    }

    @Override
    public List<Permission> findAll() {
        return permissionRepository.findAll().stream()
            .map(this::toEntity)
            .collect(Collectors.toList());
    }

    @Override
    public List<Permission> findByPermissionCodes(List<String> permissionCodes) {
        return permissionRepository.findByPermissionCodeIn(permissionCodes).stream()
            .map(this::toEntity)
            .collect(Collectors.toList());
    }

    @Override
    public void delete(Permission permission) {
        permissionRepository.delete(toDo(permission));
    }

    /**
     * DO 转 Entity
     */
    private Permission toEntity(PermissionDo doEntity) {
        if (doEntity == null) {
            return null;
        }
        Permission entity = new Permission();
        entity.setId(doEntity.getId());
        entity.setPermissionCode(doEntity.getPermissionCode());
        entity.setName(doEntity.getName());
        entity.setDescription(doEntity.getDescription());
        entity.setCategory(doEntity.getCategory());
        entity.setCreatedAt(doEntity.getCreatedAt());
        entity.setUpdatedAt(doEntity.getUpdatedAt());
        return entity;
    }

    /**
     * Entity 转 DO
     */
    private PermissionDo toDo(Permission entity) {
        if (entity == null) {
            return null;
        }
        PermissionDo doEntity = new PermissionDo();
        if (entity.getId() != null) {
            doEntity.setId(entity.getId());
        }
        doEntity.setPermissionCode(entity.getPermissionCode());
        doEntity.setName(entity.getName());
        doEntity.setDescription(entity.getDescription());
        doEntity.setCategory(entity.getCategory());
        return doEntity;
    }
}