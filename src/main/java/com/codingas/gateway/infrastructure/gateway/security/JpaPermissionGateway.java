package com.codingas.gateway.infrastructure.gateway.security;

import com.codingas.gateway.domain.security.entity.Permission;
import com.codingas.gateway.domain.security.gateway.PermissionGateway;
import com.codingas.gateway.domain.security.repository.PermissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class JpaPermissionGateway implements PermissionGateway {

    private final PermissionRepository permissionRepository;

    @Override
    public Permission save(Permission permission) {
        return permissionRepository.save(permission);
    }

    @Override
    public Optional<Permission> findById(Long id) {
        return permissionRepository.findById(id);
    }

    @Override
    public Optional<Permission> findByPermissionCode(String permissionCode) {
        return permissionRepository.findByPermissionCode(permissionCode);
    }

    @Override
    public List<Permission> findAll() {
        return permissionRepository.findAll();
    }

    @Override
    public List<Permission> findByPermissionCodes(List<String> permissionCodes) {
        return permissionRepository.findByPermissionCodeIn(permissionCodes);
    }

    @Override
    public void delete(Permission permission) {
        permissionRepository.delete(permission);
    }
}
