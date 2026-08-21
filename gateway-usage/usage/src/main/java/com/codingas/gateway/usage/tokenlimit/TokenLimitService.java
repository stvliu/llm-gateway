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
package com.codingas.gateway.usage.tokenlimit;

import com.codingas.gateway.usage.dto.TokenLimitCreateRequest;
import com.codingas.gateway.usage.dto.TokenLimitQueryRequest;
import com.codingas.gateway.usage.dto.TokenLimitResponse;
import com.codingas.gateway.usage.dto.TokenLimitUpdateRequest;
import com.codingas.gateway.common.dto.PageResponse;

/**
 * Token 限额应用服务接口
 *
 * <p>处理 Token 限额管理的业务逻辑。</p>
 */
public interface TokenLimitService {

    /**
     * 创建 Token 限额
     */
    TokenLimitResponse create(TokenLimitCreateRequest request);

    /**
     * 根据 ID 获取 Token 限额
     */
    TokenLimitResponse getById(Long id);

    /**
     * 查询 Token 限额列表
     */
    PageResponse<TokenLimitResponse> query(TokenLimitQueryRequest request);

    /**
     * 更新 Token 限额
     */
    TokenLimitResponse update(Long id, TokenLimitUpdateRequest request);

    /**
     * 删除 Token 限额（软删除）
     */
    void delete(Long id);

    /**
     * 重置已使用量
     */
    TokenLimitResponse resetUsage(Long id);
}
