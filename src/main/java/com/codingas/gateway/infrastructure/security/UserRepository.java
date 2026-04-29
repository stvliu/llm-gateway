package com.codingas.gateway.infrastructure.security;

import com.codingas.gateway.domain.security.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserDo, Long> {
    Optional<UserDo> findByUserCode(String userCode);
    Optional<UserDo> findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByUsername(String username);
}
