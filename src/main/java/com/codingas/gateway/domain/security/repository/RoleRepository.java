package com.codingas.gateway.domain.security.repository;

import com.codingas.gateway.domain.security.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByRoleCode(String roleCode);
    List<Role> findByRoleCodeIn(List<String> roleCodes);
    boolean existsByRoleCode(String roleCode);
}
