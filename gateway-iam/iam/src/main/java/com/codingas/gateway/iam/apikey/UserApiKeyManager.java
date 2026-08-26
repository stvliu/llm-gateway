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

import java.util.List;

/**
 * 用户 API Key 管理服务接口
 *
 * <p>出入参采用实体 {@link UserApiKey} 与轻量用例对象，
 * HTTP 契约（Request/Response DTO）由 web 层负责转换。</p>
 */
public interface UserApiKeyManager {

    /**
     * 创建 API Key
     *
     * @param apiKey Key 实体（承载 userId/applicationId/name）
     * @return 创建后的 Key 实体（含仅此一次可见的明文 keyPlain）
     */
    UserApiKey create(UserApiKey apiKey);

    /**
     * 按用户 ID 查询 Key 列表
     *
     * @param userId 用户 ID
     * @return Key 实体列表
     */
    List<UserApiKey> findByUserId(Long userId);

    /**
     * 查询所有非删除状态的 Key（管理员用）
     *
     * @return Key 实体列表
     */
    List<UserApiKey> findAllNonDeleted();

    /**
     * 按应用 ID 查询 Key（管理面：应用详情页查看其下 Key）
     *
     * @param applicationId 应用 ID
     * @return Key 实体列表
     */
    List<UserApiKey> findByApplicationId(Long applicationId);

    /**
     * 按 ID 查询 Key
     *
     * @param id Key ID
     * @return Key 实体
     */
    UserApiKey getById(Long id);

    /**
     * 按 ID 查询 Key 详情（含明文，详情页返回）
     *
     * @param id Key ID
     * @return Key 实体
     */
    UserApiKey getDetailById(Long id);

    /**
     * 更新 Key
     *
     * @param id      Key ID
     * @param command 更新用例入参（仅非 null 字段生效）
     * @return 更新后的 Key 实体
     */
    UserApiKey update(Long id, UserApiKey apiKey);

    /**
     * 删除 Key（逻辑删除）
     *
     * @param id Key ID
     */
    void delete(Long id);
}
