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
package com.codingas.gateway.iam.apikey;

import com.codingas.gateway.iam.apikey.UserApiKey;

import java.util.List;
import java.util.Optional;

/**
 * 用户 API Key 领域网关接口
 */
public interface UserApiKeyGateway {

    /** 按 ID 查找 */
    Optional<UserApiKey> findById(Long id);

    /** 按用户 ID 查找 */
    List<UserApiKey> findByUserId(Long userId);

    /** 按应用 ID 查找（管理面：应用详情页查看其下 Key） */
    List<UserApiKey> findByApplicationId(Long applicationId);

    /** 查询所有非删除状态的 Key（管理员用） */
    List<UserApiKey> findAllNonDeleted();

    /** 按 Key 前缀查找（认证用） */
    Optional<UserApiKey> findByKeyPrefix(String keyPrefix);

    /** 保存（含渠道关联） */
    UserApiKey save(UserApiKey userApiKey);

    /** 删除 */
    void delete(UserApiKey userApiKey);
}