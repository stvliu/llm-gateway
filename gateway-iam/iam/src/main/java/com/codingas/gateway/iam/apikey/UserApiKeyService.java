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

import com.codingas.gateway.iam.dto.UserApiKeyCreateRequest;
import com.codingas.gateway.iam.dto.UserApiKeyCreateResponse;
import com.codingas.gateway.iam.dto.UserApiKeyDetailResponse;
import com.codingas.gateway.iam.dto.UserApiKeyResponse;
import com.codingas.gateway.iam.dto.UserApiKeyUpdateRequest;

import java.util.List;

/**
 * 用户 API Key 应用服务接口
 */
public interface UserApiKeyService {

    UserApiKeyCreateResponse create(UserApiKeyCreateRequest request);

    List<UserApiKeyResponse> findByUserId(Long userId);

    /** 查询所有非删除状态的 Key（管理员用） */
    List<UserApiKeyResponse> findAllNonDeleted();

    /** 按应用 ID 查询 Key（管理面：应用详情页查看其下 Key） */
    List<UserApiKeyResponse> findByApplicationId(Long applicationId);

    UserApiKeyResponse getById(Long id);

    UserApiKeyDetailResponse getDetailById(Long id);

    UserApiKeyResponse update(Long id, UserApiKeyUpdateRequest request);

    void delete(Long id);
}