/*
 * Copyright © 2025-2026 codingas.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.codingas.gateway.iamdata.apikey;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * 用户 API Key JPA Repository
 */
public interface UserApiKeyJpaRepository extends JpaRepository<UserApiKeyDo, Long> {

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