package com.codingas.gateway.domain.security.repository;

import com.codingas.gateway.domain.security.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, Long> {
    Optional<Permission> findByPermissionCode(String permissionCode);
    List<Permission> findByPermissionCodeIn(List<String> permissionCodes);
}
