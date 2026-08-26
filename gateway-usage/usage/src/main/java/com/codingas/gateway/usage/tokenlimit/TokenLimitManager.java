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

import com.codingas.gateway.common.dto.PageResponse;

/**
 * Token 限额应用服务接口
 *
 * <p>处理 Token 限额管理的业务逻辑。出入参采用领域实体 {@link TokenLimit} 与轻量用例对象，
 * HTTP 契约（Request/Response DTO）由 web 层负责转换。</p>
 */
public interface TokenLimitManager {

    /**
     * 创建 Token 限额
     *
     * @param userId       用户 ID
     * @param providerId   提供商 ID（可选）
     * @param modelId      模型 ID（可选）
     * @param switchModelId 切换模型 ID（可选）
     * @param tokenLimit   限额实体（承载 limitType/maxTokens/period 等业务字段）
     * @return 创建后的限额实体
     */
    TokenLimit create(Long userId, Long providerId, Long modelId, Long switchModelId, TokenLimit tokenLimit);

    /**
     * 根据 ID 获取 Token 限额
     *
     * @param id 限额 ID
     * @return 限额实体
     */
    TokenLimit getById(Long id);

    /**
     * 查询 Token 限额列表
     *
     * @param query 查询条件
     * @return 限额实体分页
     */
    PageResponse<TokenLimit> query(TokenLimitQuery query);

    /**
     * 更新 Token 限额
     *
     * @param id           限额 ID
     * @param tokenLimit   限额实体（null 字段表示不更新）
     * @param switchModelId 切换模型 ID（可选，非 null 时更新切换模型）
     * @return 更新后的限额实体
     */
    TokenLimit update(Long id, TokenLimit tokenLimit, Long switchModelId);

    /**
     * 删除 Token 限额（软删除）
     *
     * @param id 限额 ID
     */
    void delete(Long id);

    /**
     * 重置已使用量
     *
     * @param id 限额 ID
     * @return 重置后的限额实体
     */
    TokenLimit resetUsage(Long id);
}
