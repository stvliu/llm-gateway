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

import com.codingas.gateway.common.dto.PageRequest;
import com.codingas.gateway.usage.tokenlimit.TokenLimit.TokenLimitState;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Token 限额查询条件用例入参
 *
 * <p>继承分页基类获得 page/limit/offset。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class TokenLimitQuery extends PageRequest {

    /** 关键字（匹配用户名） */
    private String keyword;

    /** 用户 ID 过滤 */
    private Long userId;

    /** 提供商 ID 过滤 */
    private Long providerId;

    /** 模型 ID 过滤 */
    private Long modelId;

    /** 状态过滤 */
    private TokenLimitState state;
}
