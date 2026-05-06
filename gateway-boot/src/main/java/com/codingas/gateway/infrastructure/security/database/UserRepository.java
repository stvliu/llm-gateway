package com.codingas.gateway.infrastructure.security.database;

import com.codingas.gateway.infrastructure.security.database.dataobject.UserDo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserDo, Long> {
    Optional<UserDo> findByEmail(String email);
    Optional<UserDo> findByUsername(String username);
    boolean existsByEmail(String email);
    boolean existsByUsername(String username);
}
