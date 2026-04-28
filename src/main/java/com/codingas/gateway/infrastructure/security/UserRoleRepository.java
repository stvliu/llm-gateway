package com.codingas.gateway.infrastructure.security;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRoleRepository extends JpaRepository<UserRoleDo, Long> {
    void deleteByUserId(Long userId);
}
