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
package com.codingas.gateway.iam.user;

import com.codingas.gateway.common.dto.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户查询条件用例入参
 *
 * <p>核心用户应用服务的查询入参，继承分页基类获得 page/limit/offset。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class UserQuery extends PageRequest {

    /** 关键字（匹配用户名/邮箱，模糊不区分大小写） */
    private String keyword;

    /** 用户状态过滤 */
    private UserState state;

    /** 角色代码过滤（预留） */
    private String roleCode;
}
