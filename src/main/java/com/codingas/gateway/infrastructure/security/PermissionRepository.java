package com.codingas.gateway.infrastructure.security;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface PermissionRepository extends JpaRepository<PermissionDo, Long> {
    Optional<PermissionDo> findByPermissionCode(String permissionCode);
    List<PermissionDo> findByPermissionCodeIn(List<String> permissionCodes);
}
