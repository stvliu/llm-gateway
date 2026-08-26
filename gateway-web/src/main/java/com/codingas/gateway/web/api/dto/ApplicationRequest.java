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
package com.codingas.gateway.web.api.dto;

import com.codingas.gateway.common.enums.FailureStrategy;
import com.codingas.gateway.iam.application.Application;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 应用创建/更新请求 DTO（HTTP 契约）
 *
 * <p>承载应用根实体可编辑字段：code（全局唯一）、name、description、timeout。
 * state 由后端管理（创建时默认 ACTIVE），不通过此 DTO 修改。</p>
 */
@Data
public class ApplicationRequest {

    /** 应用编码，全局唯一 */
    @NotBlank(message = "应用编码不能为空")
    private String code;

    /** 应用名称 */
    @NotBlank(message = "应用名称不能为空")
    private String name;

    /** 应用描述 */
    private String description;

    /** 请求超时秒数（0 表示用渠道默认；承接原 ResilienceProfile.timeout） */
    private int timeout;

    /** 应用级失败处理策略（不传时后端默认 FAIL_RETRY） */
    private FailureStrategy failureStrategy;

    /**
     * 转换为应用实体
     *
     * @return 应用实体
     */
    public Application toEntity() {
        Application app = new Application();
        app.setCode(code);
        app.setName(name);
        app.setDescription(description);
        app.setTimeout(timeout);
        app.setFailureStrategy(failureStrategy);
        return app;
    }
}
