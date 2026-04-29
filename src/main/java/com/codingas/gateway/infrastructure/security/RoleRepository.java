package com.codingas.gateway.infrastructure.security;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<RoleDo, Long> {
    Optional<RoleDo> findByRoleCode(String roleCode);
    List<RoleDo> findByRoleCodeIn(List<String> roleCodes);
    boolean existsByRoleCode(String roleCode);
}
