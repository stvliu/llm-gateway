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

import com.codingas.gateway.settings.SystemSetting;
import lombok.Data;

/**
 * 系统设置响应 DTO（HTTP 契约）
 *
 * <p>由 {@link #from(SystemSetting)} 从 {@code SystemSetting} 实体生成，透出配置键值、
 * 分组、描述、值类型与可编辑性。</p>
 */
@Data
public class SystemSettingResponse {

    /** 设置键（业务唯一） */
    private String settingKey;

    /** 设置值（字符串形式） */
    private String settingValue;

    /** 分组名（如 AUDIT / SYNC） */
    private String groupName;

    /** 设置描述 */
    private String description;

    /** 值类型（STRING / NUMBER / BOOLEAN 等） */
    private String valueType;

    /** 是否允许运行时修改 */
    private boolean editable;

    /**
     * 从系统设置实体转换
     *
     * @param setting 系统设置实体
     * @return 系统设置响应 DTO
     */
    public static SystemSettingResponse from(SystemSetting setting) {
        SystemSettingResponse response = new SystemSettingResponse();
        response.setSettingKey(setting.getSettingKey());
        response.setSettingValue(setting.getSettingValue());
        response.setGroupName(setting.getGroupName());
        response.setDescription(setting.getDescription());
        response.setValueType(setting.getValueType());
        response.setEditable(setting.isEditable());
        return response;
    }
}
