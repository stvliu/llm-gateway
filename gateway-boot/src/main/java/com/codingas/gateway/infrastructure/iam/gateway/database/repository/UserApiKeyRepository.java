/*
 * Copyright (c) 2025 codingas.com
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE file for details.
 */
package com.codingas.gateway.infrastructure.iam.gateway.database.repository;

import com.codingas.gateway.infrastructure.iam.gateway.database.dataobject.UserApiKeyDo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * 用户 API Key JPA Repository
 */
public interface UserApiKeyRepository extends JpaRepository<UserApiKeyDo, Long> {

    Optional<UserApiKeyDo> findByKeyHash(String keyHash);

    Optional<UserApiKeyDo> findByKeyPrefix(String keyPrefix);

    @Query("SELECT u FROM UserApiKeyDo u WHERE u.userId = :userId AND u.deleted = false")
    List<UserApiKeyDo> findByUserId(@Param("userId") Long userId);

    @Query("SELECT u FROM UserApiKeyDo u WHERE u.applicationId = :applicationId AND u.deleted = false")
    List<UserApiKeyDo> findByApplicationId(@Param("applicationId") Long applicationId);

    @Query("SELECT u FROM UserApiKeyDo u WHERE u.deleted = false")
    List<UserApiKeyDo> findAllNonDeleted();

    boolean existsByKeyPrefix(String keyPrefix);
}