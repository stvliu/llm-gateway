package com.codingas.gateway.infrastructure.gateway.security;

import com.codingas.gateway.domain.security.entity.Role;
import com.codingas.gateway.domain.security.gateway.RoleGateway;
import com.codingas.gateway.domain.security.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class JpaRoleGateway implements RoleGateway {

    private final RoleRepository roleRepository;

    @Override
    public Role save(Role role) {
        return roleRepository.save(role);
    }

    @Override
    public Optional<Role> findById(Long id) {
        return roleRepository.findById(id);
    }

    @Override
    public Optional<Role> findByRoleCode(String roleCode) {
        return roleRepository.findByRoleCode(roleCode);
    }

    @Override
    public List<Role> findAll() {
        return roleRepository.findAll();
    }

    @Override
    public List<Role> findByRoleCodes(List<String> roleCodes) {
        return roleRepository.findByRoleCodeIn(roleCodes);
    }

    @Override
    public long count() {
        return roleRepository.count();
    }

    @Override
    public void delete(Role role) {
        roleRepository.delete(role);
    }

    @Override
    public boolean existsByRoleCode(String roleCode) {
        return roleRepository.existsByRoleCode(roleCode);
    }
}
