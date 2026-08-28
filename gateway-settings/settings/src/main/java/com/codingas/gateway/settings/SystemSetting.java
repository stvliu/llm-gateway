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
package com.codingas.gateway.settings;

import com.codingas.gateway.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 系统设置实体（system_settings 表对应的领域实体）
 *
 * <p>以键值对存储全局可配置项：settingKey 为业务唯一键，
 * settingValue 按 valueType 声明的类型解释，editable 标记是否允许运行时修改。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SystemSetting extends BaseEntity {

    /** 设置键（业务唯一，如 audit.retention.days） */
    private String settingKey;

    /** 设置值（字符串形式，按 valueType 解释） */
    private String settingValue;

    /** 分组名（如 AUDIT / SYNC） */
    private String groupName;

    /** 设置描述 */
    private String description;

    /** 值类型（STRING / NUMBER / BOOLEAN 等） */
    private String valueType;

    /** 是否允许运行时修改 */
    private boolean editable;
}
