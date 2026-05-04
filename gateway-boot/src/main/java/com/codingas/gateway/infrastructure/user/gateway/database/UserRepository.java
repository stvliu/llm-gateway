package com.codingas.gateway.infrastructure.user.gateway.database;

import com.codingas.gateway.infrastructure.user.gateway.database.dataobject.UserDo;
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
