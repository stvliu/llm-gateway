/*
 * Copyright (c) 2025 codingas.com
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE file for details.
 */
package com.codingas.gateway.infrastructure.iam.gateway.database.repository;

import com.codingas.gateway.infrastructure.iam.gateway.database.dataobject.UserDo;
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
