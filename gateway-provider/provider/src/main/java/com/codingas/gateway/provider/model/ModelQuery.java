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
package com.codingas.gateway.provider.model;

import com.codingas.gateway.common.dto.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 模型查询条件用例入参
 *
 * <p>继承分页基类获得 page/limit/offset。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ModelQuery extends PageRequest {

    /** 关键字（匹配展示名/模型名） */
    private String keyword;

    /** 提供商 ID 过滤（预留） */
    private Long providerId;

    /** 状态过滤（ACTIVE=未废弃/INACTIVE=已废弃） */
    private String state;

    /** 排序字段（白名单：modelName/displayName/id，默认 modelName） */
    private String sortBy = "modelName";

    /** 排序方向（ASC/DESC，默认 ASC） */
    private String sortOrder = "ASC";
}
